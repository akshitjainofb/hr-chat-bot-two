#!/bin/bash

# Test Model Configuration Script
echo "🤖 Testing Local Model Configuration"
echo "===================================="
echo ""

# Check if Ollama is running
echo "🔍 Checking if Ollama is running..."
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "✅ Ollama is running"
else
    echo "❌ Ollama is not running. Please start it with: ollama serve"
    exit 1
fi

# Get configured model from application.yml
CONFIGURED_MODEL=$(grep -A 5 "ollama:" backend/src/main/resources/application.yml | grep "model:" | sed 's/.*model: \${OLLAMA_MODEL://' | sed 's/}.*//' | tr -d ' ')
echo "📋 Configured model: $CONFIGURED_MODEL"

# Check if model is available
echo "🔍 Checking if model is available..."
if curl -s http://localhost:11434/api/tags | grep -q "\"name\":\"$CONFIGURED_MODEL\""; then
    echo "✅ Model $CONFIGURED_MODEL is available"
else
    echo "❌ Model $CONFIGURED_MODEL is not available"
    echo "📥 Download it with: ollama pull $CONFIGURED_MODEL"
    exit 1
fi

# Test model with a simple prompt
echo "🧪 Testing model with a simple prompt..."
curl -s -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d "{
    \"model\": \"$CONFIGURED_MODEL\",
    \"prompt\": \"Hello, how are you?\",
    \"stream\": false
  }" | jq -r '.response' | head -3

echo ""
echo "✅ Model test completed!"
echo ""
echo "🚀 To change the model, edit backend/src/main/resources/application.yml:"
echo "   ollama:"
echo "     model: your-new-model-name"
echo ""
echo "📥 Then download the new model:"
echo "   ollama pull your-new-model-name"
