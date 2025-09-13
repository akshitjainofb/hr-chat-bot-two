#!/bin/bash

# Test script for enhanced local model responses
echo "🧪 Testing Enhanced Local Model Responses"
echo "=========================================="

# Test different types of HR questions
echo ""
echo "1. Testing HR policy question..."
curl -s -X POST http://localhost:8080/api/local-models/test \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What are the company policies?"}' | jq -r '.data // .error // .message'

echo ""
echo "2. Testing benefits question..."
curl -s -X POST http://localhost:8080/api/local-models/test \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about employee benefits"}' | jq -r '.data // .error // .message'

echo ""
echo "3. Testing leave policy question..."
curl -s -X POST http://localhost:8080/api/local-models/test \
  -H "Content-Type: application/json" \
  -d '{"prompt": "How do I request vacation time?"}' | jq -r '.data // .error // .message'

echo ""
echo "4. Testing salary question..."
curl -s -X POST http://localhost:8080/api/local-models/test \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What is the salary structure?"}' | jq -r '.data // .error // .message'

echo ""
echo "5. Testing general HR question..."
curl -s -X POST http://localhost:8080/api/local-models/test \
  -H "Content-Type: application/json" \
  -d '{"prompt": "I need help with workplace issues"}' | jq -r '.data // .error // .message'

echo ""
echo "✅ Test completed!"
