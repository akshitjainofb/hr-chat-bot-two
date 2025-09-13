#!/bin/bash

# HR Chatbot Local Setup Script
echo "🚀 HR Chatbot Local Setup Guide"
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠️  .env file not found. Please copy env.example to .env and configure your API keys."
    echo "   cp env.example .env"
    echo ""
fi

echo "📋 Prerequisites:"
echo "1. Java 17+ installed"
echo "2. Node.js 18+ installed"
echo "3. MySQL 8.0+ running locally"
echo "4. Maven installed"
echo "5. Ollama installed (for local AI models)"
echo ""

echo "🔧 Setup Steps:"
echo "1. Set up Ollama: ./setup-ollama.sh"
echo "2. Configure your API keys in the .env file (optional for local models)"
echo "3. Set up Google OAuth2 credentials"
echo "4. Create a Pinecone index"
echo "5. Start MySQL database"
echo "6. Run the backend: cd backend && mvn spring-boot:run"
echo "7. Run the frontend: cd frontend && npm start"
echo ""

echo "📖 For detailed setup instructions, see README.md"
