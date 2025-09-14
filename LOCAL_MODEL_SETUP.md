# 🤖 Local Model Setup Guide

## 📋 Overview

The local model configuration has been simplified! Now you only need to change the model name in `application.yml` and everything will work automatically.

## 🔧 How to Change the Model

### Step 1: Edit Configuration
Open `backend/src/main/resources/application.yml` and change the model:

```yaml
# Ollama Configuration
ollama:
  base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
  model: ${OLLAMA_MODEL:llama3.2:3b}  # Change this to your preferred model
  timeout: ${OLLAMA_TIMEOUT:15}
  temperature: ${OLLAMA_TEMPERATURE:0.5}
  max-tokens: ${OLLAMA_MAX_TOKENS:500}
```

### Step 2: Download the Model
```bash
# Download your chosen model
ollama pull llama3.2:3b

# Or any other model you prefer
ollama pull llama3.1:8b
ollama pull mistral:7b
ollama pull codellama:7b
```

### Step 3: Restart the Application
```bash
# Stop the current backend
# Then restart it
cd backend
mvn spring-boot:run
```

## 🎯 Popular Model Recommendations

### **Fast & Lightweight (Good for Development)**
```yaml
model: llama3.2:3b  # ~2GB, very fast
model: llama3.2:1b  # ~1GB, extremely fast
model: phi3:mini     # ~2GB, Microsoft's efficient model
```

### **Balanced Performance (Recommended)**
```yaml
model: llama3.1:8b   # ~4.7GB, good balance
model: mistral:7b    # ~4.1GB, excellent quality
model: codellama:7b  # ~3.8GB, great for code
```

### **High Quality (If you have resources)**
```yaml
model: llama3.1:70b  # ~40GB, very high quality
model: llama3.1:13b  # ~7.3GB, high quality
model: mistral:8x7b  # ~46GB, mixture of experts
```

## 🧪 Testing Your Model

### Quick Test
```bash
# Run the test script
./test-model.sh
```

### Manual Test
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Test your model
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama3.2:3b",
    "prompt": "Hello, how are you?",
    "stream": false
  }'
```

## 🔍 Troubleshooting

### Model Not Found
```bash
# Check available models
ollama list

# Download the model
ollama pull your-model-name

# Verify it's available
ollama list | grep your-model-name
```

### Ollama Not Running
```bash
# Start Ollama
ollama serve

# Or run in background
nohup ollama serve > ollama.log 2>&1 &
```

### Model Too Slow
- Try a smaller model (3b instead of 7b)
- Reduce max-tokens in application.yml
- Check available RAM

### Model Not Responding
- Check if the model is fully downloaded
- Restart Ollama: `pkill ollama && ollama serve`
- Check logs: `tail -f ollama.log`

## 📊 Model Comparison

| Model | Size | Speed | Quality | Use Case |
|-------|------|-------|---------|----------|
| llama3.2:1b | ~1GB | ⚡⚡⚡ | ⭐⭐ | Quick responses |
| llama3.2:3b | ~2GB | ⚡⚡ | ⭐⭐⭐ | Balanced |
| llama3.1:8b | ~4.7GB | ⚡ | ⭐⭐⭐⭐ | High quality |
| mistral:7b | ~4.1GB | ⚡ | ⭐⭐⭐⭐ | Excellent quality |
| codellama:7b | ~3.8GB | ⚡ | ⭐⭐⭐⭐ | Code-focused |

## 🚀 Production Tips

### For Deployment
1. **Choose a model that fits your server resources**
2. **Set appropriate timeouts** in application.yml
3. **Monitor memory usage** during peak times
4. **Consider using a smaller model** for faster responses

### Environment Variables
```bash
# Override model via environment variable
export OLLAMA_MODEL=llama3.1:8b

# Or set in your deployment platform
OLLAMA_MODEL=llama3.1:8b
```

## ✅ What Changed

### Before (Complex)
- Multiple model configurations
- Complex model selection logic
- Hard-coded model mappings
- Difficult to change models

### After (Simple)
- Single model configuration in application.yml
- Direct model usage from Ollama
- No complex mappings
- Easy to change models

## 🎉 Benefits

1. **Simple Configuration** - Just change one line
2. **No Default Models** - Uses exactly what you specify
3. **Direct Ollama Integration** - No complex mappings
4. **Easy Testing** - Built-in test script
5. **Clear Logging** - Shows exactly which model is being used

## 📝 Example Workflow

```bash
# 1. Choose your model
# Edit application.yml: model: llama3.1:8b

# 2. Download the model
ollama pull llama3.1:8b

# 3. Test it works
./test-model.sh

# 4. Start your application
cd backend && mvn spring-boot:run

# 5. Use the chatbot - it will use your chosen model!
```

That's it! Your chatbot will now use exactly the model you specify in the configuration file.
