"""
server.py – Main Flask entry-point for the Video Downloader backend.

Endpoints
---------
POST  /api/info              – Return video metadata for a URL
POST  /api/download          – Trigger a download, return a redirect download URL
GET   /api/download/direct   – Stream a file back to the client
GET   /api/formats           – List available quality formats for a URL
GET   /health                – Health-check / readiness probe

Error contract
--------------
All error responses are JSON:  {"error": "<message>"}
"""

from __future__ import annotations

import ipaddress
import logging
import mimetypes
import os
import socket
import threading
import time
import uuid
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

from flask import Flask, jsonify, request, send_file, abort, Response
from flask_cors import CORS

import config
import downloader as dl

# ── Logging ───────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

# ── App bootstrap ─────────────────────────────────────────────────────────────
app = Flask(__name__)
CORS(app, origins=config.CORS_ORIGINS)

# Ensure the downloads directory exists at startup
os.makedirs(config.DOWNLOAD_DIR, exist_ok=True)

# ── In-memory job registry  ───────────────────────────────────────────────────
# { job_id: {"status": ..., "filepath": ..., "error": ..., "created_at": ...} }
_jobs: dict[str, dict[str, Any]] = {}
_jobs_lock = threading.Lock()


# ── Helpers ───────────────────────────────────────────────────────────────────

def _err(message: str, status: int = 400) -> tuple[Response, int]:
    return jsonify({"error": message}), status


def _is_safe_url(url: str) -> bool:
    """Validate that the target URL is http/https and does not resolve to private or loopback IP ranges."""
    try:
        parsed = urlparse(url)
        if parsed.scheme not in ("http", "https"):
            return False
        hostname = parsed.hostname
        if not hostname:
            return False
        if hostname.lower() in ("localhost", "127.0.0.1", "::1", "0.0.0.0"):
            return False
        addr_info = socket.getaddrinfo(hostname, None)
        for entry in addr_info:
            ip_str = entry[4][0]
            ip = ipaddress.ip_address(ip_str)
            if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved or ip.is_multicast:
                return False
        return True
    except Exception:
        return False


def _get_request_url() -> str | None:
    """Extract and validate the 'url' field from the JSON request body."""
    data = request.get_json(silent=True) or {}
    url = (data.get("url") or "").strip()
    return url or None


def _new_job_id() -> str:
    return uuid.uuid4().hex


def _register_job(job_id: str) -> None:
    with _jobs_lock:
        _jobs[job_id] = {
            "status":     "pending",
            "filepath":   None,
            "filename":   None,
            "error":      None,
            "created_at": time.time(),
        }


def _update_job(job_id: str, **kwargs: Any) -> None:
    with _jobs_lock:
        if job_id in _jobs:
            _jobs[job_id].update(kwargs)


def _get_job(job_id: str) -> dict | None:
    with _jobs_lock:
        return dict(_jobs.get(job_id, {}))


# ── Background download worker ────────────────────────────────────────────────

def _run_download(job_id: str, url: str, fmt_id: str, download_type: str) -> None:
    """Run inside a daemon thread; updates the job registry when done."""
    _update_job(job_id, status="running")
    try:
        output_dir = os.path.join(config.DOWNLOAD_DIR, job_id)
        if download_type == "mp3":
            filepath = dl.download_audio(url, output_dir)
        else:
            filepath = dl.download_video(url, fmt_id, output_dir)

        # Basic size guard
        size = os.path.getsize(filepath)
        if size > config.MAX_FILE_SIZE:
            os.remove(filepath)
            raise ValueError(
                f"File ({size / (1024**2):.1f} MB) exceeds the "
                f"{config.MAX_FILE_SIZE // (1024**2)} MB limit."
            )

        _update_job(
            job_id,
            status="done",
            filepath=filepath,
            filename=os.path.basename(filepath),
        )
        logger.info("Job %s finished: %s", job_id, filepath)

    except (ValueError, RuntimeError, FileNotFoundError) as exc:
        logger.error("Job %s failed: %s", job_id, exc)
        _update_job(job_id, status="error", error=str(exc))
    except Exception as exc:  # noqa: BLE001
        logger.exception("Job %s unexpected error", job_id)
        _update_job(job_id, status="error", error=str(exc))


# ── Stale-file cleanup ────────────────────────────────────────────────────────

def _cleanup_loop() -> None:
    """Daemon thread: remove finished jobs and their files older than FILE_TTL."""
    while True:
        time.sleep(300)  # run every 5 minutes
        cutoff = time.time() - config.FILE_TTL_SECONDS
        with _jobs_lock:
            stale = [
                jid
                for jid, job in _jobs.items()
                if job["created_at"] < cutoff and job["status"] in ("done", "error")
            ]
        for jid in stale:
            job = _get_job(jid)
            fp = job.get("filepath")
            if fp and os.path.isfile(fp):
                try:
                    os.remove(fp)
                    # Also remove the per-job sub-directory if empty
                    job_dir = os.path.dirname(fp)
                    if not os.listdir(job_dir):
                        os.rmdir(job_dir)
                    logger.info("Cleaned up file for job %s", jid)
                except OSError:
                    pass
            with _jobs_lock:
                _jobs.pop(jid, None)


threading.Thread(target=_cleanup_loop, daemon=True, name="cleanup").start()


# ═══════════════════════════════════════════════════════════════════════════════
# Routes
# ═══════════════════════════════════════════════════════════════════════════════

@app.route("/health", methods=["GET"])
def health() -> tuple[Response, int]:
    """Lightweight readiness probe."""
    return (
        jsonify(
            {
                "status":     "ok",
                "download_dir": config.DOWNLOAD_DIR,
                "supported_platforms": config.SUPPORTED_PLATFORMS,
            }
        ),
        200,
    )


# ── /api/info ─────────────────────────────────────────────────────────────────

@app.route("/api/info", methods=["POST"])
def api_info() -> tuple[Response, int]:
    """
    Request body (JSON)
    -------------------
    { "url": "<video URL>" }

    Response (JSON)
    ---------------
    {
        "id":              str,
        "title":           str,
        "thumbnail":       str,
        "duration":        int,       # seconds
        "duration_string": str,       # "MM:SS" or "H:MM:SS"
        "uploader":        str | null,
        "view_count":      int | null,
        "like_count":      int | null,
        "description":     str,
        "webpage_url":     str,
        "extractor":       str,
        "formats":         [ { "id", "quality", "ext", ... } ]
    }
    """
    url = _get_request_url()
    if not url:
        return _err("'url' field is required.")
    if not _is_safe_url(url):
        return _err("Invalid or restricted URL target.", 400)

    try:
        info = dl.extract_info(url)
        formats = dl.get_formats(url)
        # Replace raw yt-dlp format list with our clean version
        info["formats"] = formats
        return jsonify(info), 200
    except ValueError as exc:
        return _err(str(exc), 422)
    except RuntimeError as exc:
        return _err(str(exc), 500)
    except Exception as exc:  # noqa: BLE001
        logger.exception("Unexpected error in /api/info")
        return _err(f"Internal error: {exc}", 500)


# ── /api/formats ──────────────────────────────────────────────────────────────

@app.route("/api/formats", methods=["GET"])
def api_formats() -> tuple[Response, int]:
    """
    Query parameters
    ----------------
    url=<video URL>

    Response (JSON)
    ---------------
    { "formats": [ { "id", "quality", "ext", "filesize", "filesize_str", "format_label" } ] }
    """
    url = (request.args.get("url") or "").strip()
    if not url:
        return _err("'url' query parameter is required.")
    if not _is_safe_url(url):
        return _err("Invalid or restricted URL target.", 400)

    try:
        formats = dl.get_formats(url)
        return jsonify({"formats": formats}), 200
    except ValueError as exc:
        return _err(str(exc), 422)
    except RuntimeError as exc:
        return _err(str(exc), 500)
    except Exception as exc:  # noqa: BLE001
        logger.exception("Unexpected error in /api/formats")
        return _err(f"Internal error: {exc}", 500)


# ── /api/download ─────────────────────────────────────────────────────────────

@app.route("/api/download", methods=["POST"])
def api_download() -> tuple[Response, int]:
    """
    Start an asynchronous download job.

    Request body (JSON)
    -------------------
    {
        "url":       "<video URL>",
        "format_id": "<yt-dlp format id>",   # use "bestaudio" for MP3
        "type":      "mp4" | "mp3"
    }

    Response (JSON) – 202 Accepted
    --------------------------------
    {
        "job_id":       str,
        "status":       "pending",
        "poll_url":     "/api/download/<job_id>/status",
        "download_url": "/api/download/direct?job_id=<job_id>"
    }
    """
    data = request.get_json(silent=True) or {}
    url       = (data.get("url") or "").strip()
    fmt_id    = (data.get("format_id") or "best").strip()
    dl_type   = (data.get("type") or "mp4").strip().lower()

    if not url:
        return _err("'url' field is required.")
    if not _is_safe_url(url):
        return _err("Invalid or restricted URL target.", 400)
    if dl_type not in ("mp4", "mp3"):
        return _err("'type' must be 'mp4' or 'mp3'.")

    job_id = _new_job_id()
    _register_job(job_id)

    thread = threading.Thread(
        target=_run_download,
        args=(job_id, url, fmt_id, dl_type),
        daemon=True,
        name=f"dl-{job_id[:8]}",
    )
    thread.start()

    return (
        jsonify(
            {
                "job_id":       job_id,
                "status":       "pending",
                "poll_url":     f"/api/download/{job_id}/status",
                "download_url": f"/api/download/direct?job_id={job_id}",
            }
        ),
        202,
    )


# ── /api/download/<job_id>/status ────────────────────────────────────────────

@app.route("/api/download/<job_id>/status", methods=["GET"])
def api_download_status(job_id: str) -> tuple[Response, int]:
    """
    Poll the status of a download job.

    Response (JSON)
    ---------------
    {
        "job_id":       str,
        "status":       "pending" | "running" | "done" | "error",
        "filename":     str | null,
        "download_url": str | null,   # populated when status == "done"
        "error":        str | null
    }
    """
    job = _get_job(job_id)
    if not job:
        return _err("Job not found.", 404)

    payload: dict[str, Any] = {
        "job_id":       job_id,
        "status":       job["status"],
        "filename":     job.get("filename"),
        "download_url": None,
        "error":        job.get("error"),
    }
    if job["status"] == "done":
        payload["download_url"] = f"/api/download/direct?job_id={job_id}"

    return jsonify(payload), 200


# ── /api/download/direct ─────────────────────────────────────────────────────

@app.route("/api/download/direct", methods=["GET"])
def api_download_direct() -> Any:
    """
    Stream a completed download back to the client.

    Query parameters
    ----------------
    job_id=<str>   (preferred)
    path=<str>     (legacy – absolute path; only usable server-side)

    The response uses Flask's send_file which sets the correct
    Content-Type, Content-Disposition, and Content-Length headers.
    """
    job_id = (request.args.get("job_id") or "").strip()
    raw_path = (request.args.get("path") or "").strip()

    if job_id:
        job = _get_job(job_id)
        if not job:
            return _err("Job not found.", 404)
        if job["status"] != "done":
            return _err(f"Download not ready (status: {job['status']}).", 202)
        filepath = job.get("filepath")
    elif raw_path:
        # Security: ensure the resolved path lives inside DOWNLOAD_DIR
        abs_download = os.path.abspath(config.DOWNLOAD_DIR)
        filepath = os.path.abspath(raw_path)
        if os.path.commonpath([filepath, abs_download]) != abs_download:
            abort(403)
    else:
        return _err("Either 'job_id' or 'path' query parameter is required.")

    if not filepath or not os.path.isfile(filepath):
        return _err("File not found on server.", 404)

    filename = os.path.basename(filepath)
    mime, _ = mimetypes.guess_type(filepath)
    mime = mime or "application/octet-stream"

    logger.info("Serving file: %s (%s)", filename, mime)
    return send_file(
        filepath,
        mimetype=mime,
        as_attachment=True,
        download_name=filename,
    )


# ═══════════════════════════════════════════════════════════════════════════════
# Error handlers
# ═══════════════════════════════════════════════════════════════════════════════

@app.errorhandler(404)
def not_found(exc: Exception) -> tuple[Response, int]:
    return _err("Endpoint not found.", 404)


@app.errorhandler(405)
def method_not_allowed(exc: Exception) -> tuple[Response, int]:
    return _err("Method not allowed.", 405)


@app.errorhandler(500)
def internal_error(exc: Exception) -> tuple[Response, int]:
    logger.exception("Unhandled 500 error")
    return _err("An unexpected server error occurred.", 500)


# ═══════════════════════════════════════════════════════════════════════════════
# Entry-point
# ═══════════════════════════════════════════════════════════════════════════════

if __name__ == "__main__":
    logger.info(
        "Starting Video Downloader backend on %s:%s (debug=%s)",
        config.HOST,
        config.PORT,
        config.DEBUG,
    )
    app.run(host=config.HOST, port=config.PORT, debug=config.DEBUG)
