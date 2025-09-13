#!/bin/bash

# Setup script for Ollama local models
# This script helps set up Ollama for real local model inference

echo "🦙 Setting up Ollama for Local Model Inference"
echo "=============================================="

# Check if Ollama is installed
if ! command -v ollama &> /dev/null; then
    echo "📥 Installing Ollama..."
    
    # Detect OS and install Ollama
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        echo "Installing Ollama for macOS..."
        curl -fsSL https://ollama.ai/install.sh | sh
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        echo "Installing Ollama for Linux..."
        curl -fsSL https://ollama.ai/install.sh | sh
    else
        echo "❌ Unsupported OS. Please install Ollama manually from https://ollama.ai"
        exit 1
    fi
else
    echo "✅ Ollama is already installed"
fi

# Start Ollama service
echo "🚀 Starting Ollama service..."
ollama serve &
OLLAMA_PID=$!

# Wait for Ollama to start
echo "⏳ Waiting for Ollama to start..."
sleep 5

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "❌ Failed to start Ollama service"
    exit 1
fi

echo "✅ Ollama service started successfully"

# Pull only the essential model
echo "📦 Pulling essential model..."

# Pull Llama 2 7B (good balance of speed and quality)
echo "Pulling Llama 2 7B..."
ollama pull llama2:7b

echo "✅ Model pulled successfully"

# Test the setup
echo "🧪 Testing Ollama setup..."
TEST_RESPONSE=$(curl -s -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama2:7b",
    "prompt": "Hello, how are you?",
    "stream": false
  }' | jq -r '.response' 2>/dev/null)

if [ "$TEST_RESPONSE" != "null" ] && [ ! -z "$TEST_RESPONSE" ]; then
    echo "✅ Ollama test successful!"
    echo "Sample response: $TEST_RESPONSE"
else
    echo "⚠️  Ollama test failed, but service is running"
fi

# Create a systemd service for auto-start (Linux only)
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "🔧 Creating systemd service for auto-start..."
    
    sudo tee /etc/systemd/system/ollama.service > /dev/null <<EOF
[Unit]
Description=Ollama Service
After=network.target

[Service]
Type=simple
User=ollama
Group=ollama
ExecStart=/usr/local/bin/ollama serve
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF

    sudo systemctl daemon-reload
    sudo systemctl enable ollama
    echo "✅ Systemd service created and enabled"
fi

echo ""
echo "🎉 Ollama setup complete!"
echo ""
echo "Available models:"
ollama list
echo ""
echo "To use with your HR Chatbot:"
echo "1. Make sure Ollama is running: ollama serve"
echo "2. Start your backend: cd backend && mvn spring-boot:run"
echo "3. Select 'Hugging Face (Local)' in the frontend settings"
echo ""
echo "To test manually:"
echo "  ollama run llama2:7b 'Hello, how are you?'"
echo ""
echo "To stop Ollama:"
echo "  pkill ollama"
