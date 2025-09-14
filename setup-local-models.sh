#!/bin/bash

# Setup script for local Hugging Face models
# This script helps set up and test local model inference

echo "🏠 Setting up Local Hugging Face Models for HR Chatbot"
echo "=================================================="

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java version check passed: $JAVA_VERSION"

# Create models directory
MODELS_DIR="./models"
if [ ! -d "$MODELS_DIR" ]; then
    mkdir -p "$MODELS_DIR"
    echo "✅ Created models directory: $MODELS_DIR"
else
    echo "✅ Models directory already exists: $MODELS_DIR"
fi

# Set environment variables for optimal performance
export DJL_CACHE_DIR="$(pwd)/$MODELS_DIR"
export DJL_PYTORCH_USE_MKLDNN=false
export DJL_PYTORCH_USE_MKL=false
export HF_MODEL_SIZE=medium

echo "✅ Environment variables set:"
echo "   DJL_CACHE_DIR=$DJL_CACHE_DIR"
echo "   HF_MODEL_SIZE=$HF_MODEL_SIZE"

# Check available memory
TOTAL_MEMORY=$(free -m | awk 'NR==2{printf "%.0f", $2}')
if [ "$TOTAL_MEMORY" -lt 4096 ]; then
    echo "⚠️  Warning: Less than 4GB RAM detected. Local models may run slowly."
    echo "   Consider using 'small' model size: export HF_MODEL_SIZE=small"
else
    echo "✅ Sufficient memory detected: ${TOTAL_MEMORY}MB"
fi

# Test model loading (optional)
echo ""
echo "🧪 Testing model loading..."
echo "This may take a few minutes on first run as models are downloaded..."

# Start the backend to test model loading
cd backend
echo "Starting backend to test local model loading..."
echo "Press Ctrl+C to stop after testing..."

# Run with local model configuration
mvn spring-boot:run -Dspring-boot.run.arguments="--local-models.preferred-size=small --local-models.auto-download=true" &
BACKEND_PID=$!

# Wait a bit for startup
sleep 10

# Test the local model endpoint
echo "Testing local model endpoint..."
curl -s http://localhost:8080/api/local-models/status || echo "Backend not ready yet"

# Clean up
echo ""
echo "Stopping backend..."
kill $BACKEND_PID 2>/dev/null

echo ""
echo "🎉 Setup complete!"
echo ""
echo "To use local models:"
echo "1. Start the backend: cd backend && mvn spring-boot:run"
echo "2. The models will be automatically downloaded on first use"
echo "3. Check the settings page in the frontend to select 'Hugging Face (Local)'"
echo ""
echo "Model sizes available:"
echo "  - small:  Fast, lightweight (distilbert-base-uncased)"
echo "  - medium: Balanced (microsoft/DialoGPT-medium)"
echo "  - large:  Better quality (microsoft/DialoGPT-large)"
echo "  - best:   Best quality (microsoft/DialoGPT-xlarge)"
echo ""
echo "To change model size, set environment variable:"
echo "  export HF_MODEL_SIZE=medium"
echo ""
echo "Models will be cached in: $MODELS_DIR"
