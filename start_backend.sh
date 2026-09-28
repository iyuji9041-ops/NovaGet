#!/usr/bin/env bash
# =============================================================================
# Video Downloader - Backend Quick Start Script
# =============================================================================

set -e

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/backend"

echo "🎬 Video Downloader Backend Setup"
echo "=================================="

# Check Python
if ! command -v python3 &> /dev/null; then
    echo "❌ Python3 not found. Please install Python 3.8+"
    exit 1
fi

echo "✅ Python: $(python3 --version)"

# Check pip
if ! command -v pip3 &> /dev/null; then
    echo "❌ pip3 not found. Please install pip"
    exit 1
fi

# Check ffmpeg (needed for MP3 extraction and video muxing)
if ! command -v ffmpeg &> /dev/null; then
    echo "⚠️  ffmpeg not found!"
    echo "   MP3 download and some video formats need ffmpeg."
    echo "   Install with: sudo apt install ffmpeg  (Linux)"
    echo "   Install with: brew install ffmpeg      (macOS)"
    echo ""
fi

# Install dependencies
echo ""
echo "📦 Installing Python dependencies..."
pip3 install -r "$BACKEND_DIR/requirements.txt" --quiet

echo "✅ Dependencies installed!"

# Get local IP for real device usage
LOCAL_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "127.0.0.1")

echo ""
echo "🚀 Starting backend server..."
echo "=================================="
echo "   Local URL  : http://localhost:5000"
echo "   Emulator   : http://10.0.2.2:5000"
echo "   Real Device: http://${LOCAL_IP}:5000"
echo ""
echo "📱 In the Android app → Settings → change Backend URL to:"
echo "   Real device: http://${LOCAL_IP}:5000"
echo ""
echo "Press Ctrl+C to stop"
echo "=================================="

python3 "$BACKEND_DIR/server.py"
