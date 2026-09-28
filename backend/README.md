# Video Downloader – Flask Backend

A production-ready Python Flask backend that wraps **yt-dlp** to download videos and audio from YouTube, Instagram, TikTok, Twitter/X, Facebook, Reddit, Vimeo, Dailymotion, and [hundreds of other sites](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md).

---

## Prerequisites

| Tool | Minimum version |
|------|----------------|
| Python | 3.9+ |
| pip | 21+ |
| ffmpeg | Any recent release (required for merging video+audio and MP3 conversion) |

Install ffmpeg:
```bash
# Ubuntu / Debian
sudo apt install ffmpeg

# macOS (Homebrew)
brew install ffmpeg

# Windows (Chocolatey)
choco install ffmpeg
```

---

## Installation

```bash
# 1. Clone / navigate to the project
cd /workspace/downloader/backend

# 2. (Recommended) Create a virtual environment
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate

# 3. Install Python dependencies
pip install -r requirements.txt
```

---

## Running the Server

```bash
python server.py
```

The server starts on **`http://0.0.0.0:5000`** by default.

### Environment variable overrides

| Variable | Default | Description |
|----------|---------|-------------|
| `HOST` | `0.0.0.0` | Bind address |
| `PORT` | `5000` | Bind port |
| `DEBUG` | `false` | Flask debug mode |
| `DOWNLOAD_DIR` | `./downloads` | Where downloaded files are stored |
| `MAX_FILE_SIZE` | `524288000` | Max file size in bytes (500 MB) |
| `CORS_ORIGINS` | `*` | CORS allowed origins |
| `FILE_TTL_SECONDS` | `3600` | How long finished files are kept |

Example:
```bash
PORT=8080 DEBUG=true python server.py
```

---

## API Reference

### `GET /health`
Health check / readiness probe.

**Response 200**
```json
{
  "status": "ok",
  "download_dir": "./downloads",
  "supported_platforms": ["YouTube", "Instagram", "TikTok", ...]
}
```

---

### `POST /api/info`
Fetch metadata for a video URL.

**Request body**
```json
{ "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ" }
```

**Response 200**
```json
{
  "id": "dQw4w9WgXcQ",
  "title": "Rick Astley - Never Gonna Give You Up",
  "thumbnail": "https://...",
  "duration": 212,
  "duration_string": "3:32",
  "uploader": "Rick Astley",
  "view_count": 1400000000,
  "like_count": 16000000,
  "description": "...",
  "webpage_url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "extractor": "Youtube",
  "formats": [
    { "id": "137", "quality": "1080p", "ext": "mp4", "filesize": 120000000, "filesize_str": "114.4 MB", "format_label": "1080p (MP4)" },
    { "id": "bestaudio", "quality": "Audio Only", "ext": "mp3", "filesize": null, "filesize_str": null, "format_label": "Audio Only (MP3)" }
  ]
}
```

---

### `GET /api/formats?url=<video_url>`
List all available quality formats for a URL.

**Example**
```
GET /api/formats?url=https://youtu.be/dQw4w9WgXcQ
```

**Response 200**
```json
{
  "formats": [
    { "id": "313", "quality": "4K (2160p)", "ext": "webm", "format_label": "4K (2160p) (WEBM)", "filesize": 850000000, "filesize_str": "810.6 MB", "fps": 30 },
    { "id": "137", "quality": "1080p",      "ext": "mp4",  "format_label": "1080p (MP4)",        "filesize": 120000000, "filesize_str": "114.4 MB", "fps": 30 },
    { "id": "136", "quality": "720p",       "ext": "mp4",  "format_label": "720p (MP4)",          "filesize": 60000000,  "filesize_str": "57.2 MB",  "fps": 30 },
    { "id": "bestaudio", "quality": "Audio Only", "ext": "mp3", "format_label": "Audio Only (MP3)" }
  ]
}
```

---

### `POST /api/download`
Start an **asynchronous** download job. Returns immediately with a `job_id`.

**Request body**
```json
{
  "url":       "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "format_id": "137",
  "type":      "mp4"
}
```

| Field | Values | Notes |
|-------|--------|-------|
| `url` | any supported URL | required |
| `format_id` | format `id` from `/api/formats` | use `"bestaudio"` for MP3 |
| `type` | `"mp4"` \| `"mp3"` | required |

**Response 202 Accepted**
```json
{
  "job_id":       "a1b2c3d4...",
  "status":       "pending",
  "poll_url":     "/api/download/a1b2c3d4.../status",
  "download_url": "/api/download/direct?job_id=a1b2c3d4..."
}
```

---

### `GET /api/download/<job_id>/status`
Poll a job's current state.

**Response 200**
```json
{
  "job_id":       "a1b2c3d4...",
  "status":       "done",
  "filename":     "Rick Astley - Never Gonna Give You Up.mp4",
  "download_url": "/api/download/direct?job_id=a1b2c3d4...",
  "error":        null
}
```

| `status` value | Meaning |
|---------------|---------|
| `pending` | Queued, not started yet |
| `running` | yt-dlp is downloading |
| `done` | File ready – use `download_url` |
| `error` | Failed – see `error` field |

---

### `GET /api/download/direct?job_id=<id>`
**Stream** the completed file directly to the client with proper `Content-Disposition` headers.

> Use this URL as the `href` for a download link or `downloadManager.enqueue()` on Android.

---

## Error Responses

All errors return JSON with an `"error"` key:

```json
{ "error": "Description of what went wrong." }
```

| HTTP status | Meaning |
|------------|---------|
| 400 | Bad request (missing / invalid field) |
| 202 | Download not ready yet (when polling direct before `done`) |
| 403 | Path traversal attempt |
| 404 | Job or endpoint not found |
| 405 | Wrong HTTP method |
| 422 | yt-dlp could not process the URL |
| 500 | Unexpected server error |

---

## Connecting from Android

### Android Emulator
The emulator maps `10.0.2.2` to your host machine's `localhost`:

```
Base URL: http://10.0.2.2:5000
```

Example Retrofit base URL:
```kotlin
private const val BASE_URL = "http://10.0.2.2:5000/"
```

### Real Physical Device
Find your computer's local IP address:

```bash
# Linux / macOS
ip addr show   # or: hostname -I

# Windows
ipconfig
```

Then use that IP in your app (e.g. `192.168.1.42`):
```
Base URL: http://192.168.1.42:5000
```

> **Tip:** Make sure your firewall allows inbound connections on port 5000, and that both devices are on the same Wi-Fi network.

### Android `network_security_config.xml` (cleartext HTTP)
Add this to allow plain HTTP in debug builds:

```xml
<!-- res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">192.168.1.42</domain>
    </domain-config>
</network-security-config>
```

Reference it in `AndroidManifest.xml`:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

---

## Typical Download Flow

```
Client                              Server
  │                                   │
  │  POST /api/info { url }           │
  │ ─────────────────────────────────>│  ← get metadata + format list
  │ <─────────────────────────────────│
  │                                   │
  │  POST /api/download               │
  │  { url, format_id, type }         │
  │ ─────────────────────────────────>│  ← returns job_id immediately (202)
  │ <─────────────────────────────────│
  │                                   │
  │  GET /api/download/<id>/status    │
  │ ─────────────────────────────────>│  ← poll every 2–3 s
  │ <─── { status: "running" } ───────│
  │                                   │
  │  GET /api/download/<id>/status    │
  │ ─────────────────────────────────>│
  │ <─── { status: "done", … } ───────│
  │                                   │
  │  GET /api/download/direct         │
  │      ?job_id=<id>                 │
  │ ─────────────────────────────────>│  ← streams the file
  │ <══════════ file bytes ═══════════│
```

---

## Project Structure

```
backend/
├── server.py        # Flask app, routes, job management
├── downloader.py    # yt-dlp wrapper (extract_info, get_formats, download_*)
├── config.py        # All configuration / env-var overrides
├── requirements.txt # Python dependencies
├── README.md        # This file
└── downloads/       # Created automatically at runtime
    └── <job_id>/
        └── <video title>.<ext>
```
