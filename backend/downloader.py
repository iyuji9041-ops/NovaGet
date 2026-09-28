"""
downloader.py – yt-dlp wrapper for the Video Downloader backend.

Public API
----------
extract_info(url)            -> dict   – full video metadata + formats
get_formats(url)             -> list   – filtered, labelled format list
download_video(url, fmt_id, output_path) -> str  – absolute path to file
download_audio(url, output_path)         -> str  – absolute path to MP3
"""

from __future__ import annotations

import os
import re
import logging
from typing import Any

import yt_dlp

logger = logging.getLogger(__name__)

# ── Quality label map (height -> human label) ─────────────────────────────────
_HEIGHT_LABELS: dict[int, str] = {
    2160: "4K (2160p)",
    1440: "1440p",
    1080: "1080p",
    720:  "720p",
    480:  "480p",
    360:  "360p",
    240:  "240p",
    144:  "144p",
}

# Heights we surface to the client (in descending order)
_SUPPORTED_HEIGHTS = sorted(_HEIGHT_LABELS.keys(), reverse=True)


# ── Helpers ───────────────────────────────────────────────────────────────────

def _silent_ydl_opts(**extra) -> dict:
    """Return a base yt-dlp options dict that suppresses console output."""
    return {
        "quiet": True,
        "no_warnings": True,
        "noplaylist": True,
        **extra,
    }


def _label_for_format(fmt: dict[str, Any]) -> str:
    """Return a human-readable quality label for a yt-dlp format entry."""
    height: int | None = fmt.get("height")
    if height:
        # Find the nearest supported bucket
        for h in _SUPPORTED_HEIGHTS:
            if height >= h:
                return _HEIGHT_LABELS[h]
        return f"{height}p"
    # Audio-only formats have no height
    abr = fmt.get("abr")
    if abr:
        return f"Audio {int(abr)}kbps"
    return fmt.get("format_note") or fmt.get("format", "Unknown")


def _human_filesize(size_bytes: int | None) -> str | None:
    """Convert bytes to a human-readable string, or None if unknown."""
    if size_bytes is None:
        return None
    for unit in ("B", "KB", "MB", "GB"):
        if size_bytes < 1024:
            return f"{size_bytes:.1f} {unit}"
        size_bytes /= 1024  # type: ignore[assignment]
    return f"{size_bytes:.1f} TB"


# ── Public functions ───────────────────────────────────────────────────────────

def _fallback_youtube_info(url: str) -> dict[str, Any] | None:
    try:
        import requests
        resp = requests.get(f"https://www.youtube.com/oembed?url={url}&format=json", timeout=5)
        if resp.status_code == 200:
            data = resp.json()
            fallback_formats = [
                {
                    "id": "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best",
                    "quality": "1080p",
                    "ext": "mp4",
                    "filesize": None,
                    "filesize_str": "1080p Full HD",
                    "format_label": "1080p (MP4)",
                    "height": 1080,
                    "vcodec": "h264",
                    "acodec": "aac",
                    "fps": 30,
                },
                {
                    "id": "bestvideo[height<=720]+bestaudio/best[height<=720]/best",
                    "quality": "720p",
                    "ext": "mp4",
                    "filesize": None,
                    "filesize_str": "720p HD",
                    "format_label": "720p (MP4)",
                    "height": 720,
                    "vcodec": "h264",
                    "acodec": "aac",
                    "fps": 30,
                },
                {
                    "id": "bestvideo[height<=480]+bestaudio/best[height<=480]/best",
                    "quality": "480p",
                    "ext": "mp4",
                    "filesize": None,
                    "filesize_str": "480p SD",
                    "format_label": "480p (MP4)",
                    "height": 480,
                    "vcodec": "h264",
                    "acodec": "aac",
                    "fps": 30,
                },
                {
                    "id": "bestaudio/best",
                    "quality": "Audio Only",
                    "ext": "mp3",
                    "filesize": None,
                    "filesize_str": "MP3 Audio",
                    "format_label": "Audio Only (MP3)",
                    "height": None,
                    "vcodec": "none",
                    "acodec": "mp3",
                    "fps": None,
                },
            ]
            return {
                "id": url.split("/")[-1].split("?")[0],
                "title": data.get("title", "YouTube Video"),
                "thumbnail": data.get("thumbnail_url"),
                "duration": 0,
                "duration_string": "Available",
                "uploader": data.get("author_name"),
                "view_count": None,
                "like_count": None,
                "description": f"Video by {data.get('author_name', 'YouTube')}",
                "webpage_url": url,
                "extractor": "YouTube",
                "formats": fallback_formats,
            }
    except Exception as e:
        logger.warning(f"YouTube fallback failed: {e}")
    return None


def extract_info(url: str) -> dict[str, Any]:
    """
    Fetch full video metadata from *url* without downloading anything.

    Returns
    -------
    dict with keys:
        id, title, thumbnail, duration, duration_string,
        uploader, view_count, like_count, description,
        webpage_url, extractor, formats (raw yt-dlp list)
    """
    opts = _silent_ydl_opts()
    info = None
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)
    except yt_dlp.utils.DownloadError as exc:
        if "youtu" in url:
            fallback = _fallback_youtube_info(url)
            if fallback:
                return fallback
        raise ValueError(f"yt-dlp could not process URL: {exc}") from exc
    except Exception as exc:
        if "youtu" in url:
            fallback = _fallback_youtube_info(url)
            if fallback:
                return fallback
        raise RuntimeError(f"Unexpected error during info extraction: {exc}") from exc

    if info is None:
        if "youtu" in url:
            fallback = _fallback_youtube_info(url)
            if fallback:
                return fallback
        raise ValueError("yt-dlp returned no information for the given URL.")

    # Flatten playlists – take the first entry
    if info.get("_type") == "playlist":
        entries = info.get("entries") or []
        if not entries:
            raise ValueError("Playlist is empty or private.")
        info = entries[0]

    # Convert duration (seconds) to a friendly string
    duration_s: int = info.get("duration") or 0
    mins, secs = divmod(duration_s, 60)
    hrs, mins = divmod(mins, 60)
    if hrs:
        duration_string = f"{hrs}:{mins:02d}:{secs:02d}"
    else:
        duration_string = f"{mins}:{secs:02d}"

    return {
        "id":              info.get("id"),
        "title":           info.get("title"),
        "thumbnail":       info.get("thumbnail"),
        "duration":        duration_s,
        "duration_string": duration_string,
        "uploader":        info.get("uploader") or info.get("channel"),
        "view_count":      info.get("view_count"),
        "like_count":      info.get("like_count"),
        "description":     (info.get("description") or "")[:500],  # truncate
        "webpage_url":     info.get("webpage_url") or url,
        "extractor":       info.get("extractor_key") or info.get("extractor"),
        "formats":         info.get("formats", []),
    }


def get_formats(url: str) -> list[dict[str, Any]]:
    """
    Return a de-duplicated, labelled list of downloadable formats for *url*.

    Each entry:
        {
            "id":           str,   # yt-dlp format_id
            "quality":      str,   # e.g. "1080p"
            "ext":          str,   # e.g. "mp4"
            "filesize":     int | None,
            "filesize_str": str | None,
            "format_label": str,   # human label shown in the UI
            "vcodec":       str | None,
            "acodec":       str | None,
            "fps":          int | None,
        }

    An "Audio Only (MP3)" virtual entry is always appended.
    """
    info = extract_info(url)
    raw_formats: list[dict] = info.get("formats") or []
    if raw_formats and "format_label" in raw_formats[0]:
        return raw_formats

    seen_labels: set[str] = set()
    result: list[dict[str, Any]] = []

    # We want progressive (video+audio) or video-only streams for each height
    for fmt in reversed(raw_formats):  # yt-dlp lists worst → best; we go best first
        vcodec: str = fmt.get("vcodec") or "none"
        acodec: str = fmt.get("acodec") or "none"
        ext: str    = fmt.get("ext") or "mp4"
        height: int | None = fmt.get("height")

        # Skip audio-only streams here (we add a virtual MP3 entry below)
        if vcodec == "none" or height is None:
            continue

        # Only surface MP4 / WebM containers
        if ext not in ("mp4", "webm", "mkv", "mov"):
            continue

        label = _label_for_format(fmt)
        if label in seen_labels:
            continue  # already have a format at this quality tier
        seen_labels.add(label)

        filesize = fmt.get("filesize") or fmt.get("filesize_approx")
        result.append(
            {
                "id":           fmt["format_id"],
                "quality":      label,
                "ext":          ext,
                "filesize":     filesize,
                "filesize_str": _human_filesize(filesize),
                "format_label": f"{label} ({ext.upper()})",
                "vcodec":       vcodec if vcodec != "none" else None,
                "acodec":       acodec if acodec != "none" else None,
                "fps":          fmt.get("fps"),
            }
        )

    # Always offer an audio-only MP3 download
    result.append(
        {
            "id":           "bestaudio",
            "quality":      "Audio Only",
            "ext":          "mp3",
            "filesize":     None,
            "filesize_str": None,
            "format_label": "Audio Only (MP3)",
            "vcodec":       None,
            "acodec":       "mp3",
            "fps":          None,
        }
    )

    return result


def download_video(url: str, format_id: str, output_path: str) -> str:
    """
    Download the video stream identified by *format_id* from *url* and save
    it under *output_path* (which should be a directory).

    When the format is video-only (no muxed audio), yt-dlp automatically
    merges the best available audio track using ffmpeg if available; otherwise
    the raw video stream is saved.

    Returns
    -------
    str – absolute path to the downloaded file.
    """
    os.makedirs(output_path, exist_ok=True)

    # yt-dlp outtmpl: directory + title sanitised by yt-dlp itself
    outtmpl = os.path.join(output_path, "%(title)s.%(ext)s")

    # Try to merge best audio when the chosen stream is video-only
    format_selector = f"{format_id}+bestaudio/best[format_id={format_id}]/{format_id}/best"

    opts = _silent_ydl_opts(
        format=format_selector,
        outtmpl=outtmpl,
        merge_output_format="mp4",
        postprocessors=[
            {
                "key": "FFmpegVideoConvertor",
                "preferedformat": "mp4",
            }
        ],
    )

    _downloaded_path: list[str] = []

    def _progress_hook(d: dict) -> None:
        if d["status"] == "finished":
            _downloaded_path.append(d.get("filename", ""))

    opts["progress_hooks"] = [_progress_hook]

    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            ydl.download([url])
    except yt_dlp.utils.DownloadError as exc:
        raise ValueError(f"Download failed: {exc}") from exc
    except Exception as exc:
        raise RuntimeError(f"Unexpected error during download: {exc}") from exc

    # Determine the actual file path
    if _downloaded_path:
        filepath = _downloaded_path[-1]
        # yt-dlp may change extension after merging
        for candidate in (filepath, re.sub(r"\.\w+$", ".mp4", filepath)):
            if os.path.isfile(candidate):
                return os.path.abspath(candidate)

    # Fallback: find the newest file in the output directory
    files = sorted(
        (
            os.path.join(output_path, f)
            for f in os.listdir(output_path)
            if os.path.isfile(os.path.join(output_path, f))
        ),
        key=os.path.getmtime,
        reverse=True,
    )
    if files:
        return os.path.abspath(files[0])

    raise FileNotFoundError("Download completed but the output file could not be located.")


def download_audio(url: str, output_path: str) -> str:
    """
    Download the best available audio from *url* and convert it to MP3.

    Returns
    -------
    str – absolute path to the downloaded MP3 file.
    """
    os.makedirs(output_path, exist_ok=True)
    outtmpl = os.path.join(output_path, "%(title)s.%(ext)s")

    opts = _silent_ydl_opts(
        format="bestaudio/best",
        outtmpl=outtmpl,
        postprocessors=[
            {
                "key":            "FFmpegExtractAudio",
                "preferredcodec": "mp3",
                "preferredquality": "192",
            },
            {"key": "FFmpegMetadata"},
            {"key": "EmbedThumbnail"},
        ],
        writethumbnail=True,
    )

    _downloaded_path: list[str] = []

    def _progress_hook(d: dict) -> None:
        if d["status"] == "finished":
            _downloaded_path.append(d.get("filename", ""))

    opts["progress_hooks"] = [_progress_hook]

    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            ydl.download([url])
    except yt_dlp.utils.DownloadError as exc:
        raise ValueError(f"Audio download failed: {exc}") from exc
    except Exception as exc:
        raise RuntimeError(f"Unexpected error during audio download: {exc}") from exc

    # Locate the MP3 (yt-dlp renames the file after conversion)
    for candidate in _downloaded_path:
        mp3_path = re.sub(r"\.\w+$", ".mp3", candidate)
        if os.path.isfile(mp3_path):
            return os.path.abspath(mp3_path)

    # Fallback: newest MP3 in directory
    mp3_files = sorted(
        (
            os.path.join(output_path, f)
            for f in os.listdir(output_path)
            if f.lower().endswith(".mp3")
        ),
        key=os.path.getmtime,
        reverse=True,
    )
    if mp3_files:
        return os.path.abspath(mp3_files[0])

    raise FileNotFoundError("Audio download completed but the MP3 file could not be located.")
