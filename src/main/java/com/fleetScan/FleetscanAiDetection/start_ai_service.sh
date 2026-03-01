#!/bin/bash

# FleetScan AI Service Startup Script
# This script starts the Python AI detection service

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🔍 FleetScan AI Service"
echo "======================"

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "❌ Virtual environment not found!"
    echo "Creating virtual environment..."
    python3 -m venv venv
    if [ $? -ne 0 ]; then
        echo "❌ Failed to create virtual environment"
        exit 1
    fi
fi

# Activate virtual environment
echo "📦 Activating virtual environment..."
source venv/bin/activate

# Check if requirements are installed
echo "📋 Checking dependencies..."
python3 -c "import fastapi, uvicorn, easyocr, ultralytics, cv2" 2>/dev/null
if [ $? -ne 0 ]; then
    echo "⚠️  Installing dependencies..."
    pip install -r fleet_ai/requirements.txt
    if [ $? -ne 0 ]; then
        echo "❌ Failed to install dependencies"
        exit 1
    fi
fi

# Check if model exists
MODEL_PATH="fleet_ai/models/plate_yolo/best.pt"
if [ ! -f "$MODEL_PATH" ]; then
    echo "⚠️  Model file not found at $MODEL_PATH"
    echo "   The service will try to download it automatically"
fi

# Start the service
echo "🚀 Starting AI service on port 8000..."
echo "   Press Ctrl+C to stop"
echo ""

python3 -m uvicorn fleet_ai.main:app --host 0.0.0.0 --port 8000 --reload
