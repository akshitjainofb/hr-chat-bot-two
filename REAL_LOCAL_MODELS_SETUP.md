# 🦙 Real Local Models Setup Guide

## Overview

This guide will help you set up **real local model inference** using Ollama, so you get actual AI-generated responses instead of dummy strings.

## 🚀 Quick Start

### 1. Install Ollama

```bash
# Run the setup script
./setup-ollama.sh
```

Or install manually:
```bash
# macOS/Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Start Ollama
ollama serve
```

### 2. Pull Essential Model

```bash
# Pull Llama 2 7B (recommended for most users)
ollama pull llama2:7b
```

### 3. Test Ollama

```bash
# Test if Ollama is working
ollama run llama2:7b "Hello, how are you?"

# Check available models
ollama list
```

### 4. Start Your Backend

```bash
cd backend
mvn spring-boot:run
```

### 5. Configure Frontend

1. Open `http://localhost:3000`
2. Go to Settings
3. Select "Hugging Face (Local)"
4. Save settings

## 🎯 How It Works

### Real Model Inference

The system now uses **Ollama** for actual local model inference:

1. **Ollama Integration**: Calls `http://localhost:11434/api/generate`
2. **Model Mapping**: Maps Hugging Face model names to Ollama models
3. **Real Responses**: Generates actual AI responses using local models
4. **Fallback**: Enhanced local responses if Ollama is unavailable

### Model Mapping

| Hugging Face Model | Ollama Model | Description |
|-------------------|--------------|-------------|
| `microsoft/DialoGPT-medium` | `llama2:7b` | Good balance of speed/quality |
| `microsoft/DialoGPT-large` | `llama2:7b` | Same as medium for consistency |
| `microsoft/DialoGPT-xlarge` | `llama2:7b` | Same as medium for consistency |
| `distilbert-base-uncased` | `llama2:7b` | Fast, lightweight |

## ⚙️ Configuration

### Environment Variables

```bash
# Ollama Configuration
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_DEFAULT_MODEL=llama2:7b
export OLLAMA_TIMEOUT=30
export OLLAMA_TEMPERATURE=0.7
export OLLAMA_MAX_TOKENS=500

# Model Size Preference
export HF_MODEL_SIZE=medium  # small, medium, large, best
```

### Application Configuration

The system automatically configures based on your `application.yml`:

```yaml
ollama:
  base-url: http://localhost:11434
  default-model: llama2:7b
  timeout: 30
  temperature: 0.7
  max-tokens: 500
```

## 🧪 Testing

### Test Local Model

```bash
# Test via API
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What are the company policies?",
    "roomId": 1
  }'
```

### Test Ollama Directly

```bash
# Test Ollama API
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama2:7b",
    "prompt": "Hello, how are you?",
    "stream": false
  }'
```

## 📊 Monitoring

### Check Service Status

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Check backend status
curl http://localhost:8080/api/local-models/status
```

### View Logs

```bash
# Backend logs
tail -f backend/logs/application.log

# Ollama logs
ollama logs
```

## 🔧 Troubleshooting

### Common Issues

1. **Port 8080 already in use**
   ```bash
   # Kill existing process
   lsof -ti:8080 | xargs kill -9
   ```

2. **Ollama not responding**
   ```bash
   # Restart Ollama
   pkill ollama
   ollama serve
   ```

3. **Model not found**
   ```bash
   # Pull the model
   ollama pull llama2:7b
   ```

4. **Slow responses**
   - Use smaller models (`llama2:7b` instead of `llama2:70b`)
   - Reduce `max-tokens` in configuration
   - Check available RAM

### Performance Tips

1. **Use the recommended model**:
   - `llama2:7b` - Fast, good for most use cases, balanced performance

2. **Optimize settings**:
   - Lower `temperature` for more focused responses
   - Reduce `max-tokens` for faster responses
   - Adjust `top_p` for response diversity

## 🎉 Expected Results

### Before (Dummy Responses)
```
I'm running locally without any API keys! As your local HR assistant...
```

### After (Real AI Responses)
```
Based on your question about company policies, I can help you understand our standard procedures. Here are some key areas I can assist with:

1. **Employee Handbook**: Our comprehensive guide covers all major policies
2. **Code of Conduct**: Expected behavior and ethical guidelines
3. **Leave Policies**: Vacation, sick leave, and personal time off
4. **Remote Work**: Guidelines for working from home
5. **Performance Reviews**: How evaluations are conducted

What specific policy would you like to know more about? [Generated locally via Ollama]
```

## 🚀 Next Steps

1. **Try different models** - Experiment with various Ollama models
2. **Customize prompts** - Modify the prompt engineering for better responses
3. **Add more models** - Pull additional models for different use cases
4. **Optimize performance** - Tune settings for your hardware

## 📝 Notes

- **First run**: Models need to be downloaded (can take several GB)
- **RAM usage**: Larger models need more memory
- **GPU support**: Ollama can use GPU if available
- **Offline**: Once models are downloaded, everything works offline

Your HR Chatbot now has **real local AI capabilities**! 🎉
