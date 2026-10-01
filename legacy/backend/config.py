"""
Configuration settings for the Video Downloader Flask backend.
"""

import os

# ── Directory Settings ─────────────────────────────────────────────────────────
DOWNLOAD_DIR = os.environ.get("DOWNLOAD_DIR", "./downloads")

# ── File Size Limits ───────────────────────────────────────────────────────────
MAX_FILE_SIZE = int(os.environ.get("MAX_FILE_SIZE", 500 * 1024 * 1024))  # 500 MB

# ── Server Settings ────────────────────────────────────────────────────────────
HOST = os.environ.get("HOST", "127.0.0.1")
PORT = int(os.environ.get("PORT", 5000))
DEBUG = os.environ.get("DEBUG", "false").lower() == "true"

# ── CORS Settings ──────────────────────────────────────────────────────────────
# Restrict to local origin by default instead of wildcard '*'
CORS_ORIGINS = os.environ.get("CORS_ORIGINS", "http://127.0.0.1:5000,http://localhost:5000")

# ── yt-dlp / Download Behaviour ───────────────────────────────────────────────
# Maximum concurrent downloads (reserved for future rate-limiting use)
MAX_CONCURRENT_DOWNLOADS = int(os.environ.get("MAX_CONCURRENT_DOWNLOADS", 3))

# How long (seconds) a finished file is kept before the cleanup task removes it
FILE_TTL_SECONDS = int(os.environ.get("FILE_TTL_SECONDS", 3600))  # 1 hour

# ── Supported Platforms (informational – yt-dlp handles the real matching) ────
SUPPORTED_PLATFORMS = [
    "YouTube",
    "Instagram",
    "TikTok",
    "Twitter / X",
    "Facebook",
    "Reddit",
    "Vimeo",
    "Dailymotion",
]
