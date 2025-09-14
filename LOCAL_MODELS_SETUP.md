# Local Hugging Face Models Setup

This guide explains how to set up and use local Hugging Face models in your HR Chatbot, eliminating the need for API keys and ensuring complete privacy.

## 🏠 Why Use Local Models?

- **No API Keys Required**: Run models completely offline
- **Complete Privacy**: Your data never leaves your server
- **Cost Effective**: No per-request charges
- **Customizable**: Use any Hugging Face model
- **Offline Capable**: Works without internet after initial setup

## 📋 Prerequisites

- Java 17 or higher
- At least 4GB RAM (8GB+ recommended)
- 2-5GB free disk space for model caching
- Maven 3.6+

## 🚀 Quick Setup

### 1. Run the Setup Script

```bash
./setup-local-models.sh
```

This script will:
- Check Java version
- Create model cache directory
- Set optimal environment variables
- Test model loading

### 2. Start the Backend

```bash
cd backend
mvn spring-boot:run
```

The first run will download models automatically (may take 5-15 minutes).

### 3. Configure in Frontend

1. Open the frontend: `http://localhost:3000`
2. Go to Settings
3. Select "Hugging Face (Local)" as your preferred model
4. Save settings

## ⚙️ Configuration Options

### Environment Variables

```bash
# Model cache directory
export LOCAL_MODEL_CACHE_DIR=./models

# Model size preference (small, medium, large, best)
export HF_MODEL_SIZE=medium

# Auto-download models
export LOCAL_MODEL_AUTO_DOWNLOAD=true

# Maximum models in memory
export LOCAL_MODEL_MAX_IN_MEMORY=2

# Memory limit (MB)
export LOCAL_MODEL_MEMORY_LIMIT=2048

# Use GPU if available
export LOCAL_MODEL_USE_GPU=false
```

### Application Configuration

Add to `backend/src/main/resources/application.yml`:

```yaml
local-models:
  cache-dir: ./models
  auto-download: true
  max-models-in-memory: 2
  preferred-size: medium
  use-gpu: false
  memory-limit-mb: 2048
```

## 🎯 Available Models

| Size | Model | Description | Memory | Speed |
|------|-------|-------------|--------|-------|
| **small** | `distilbert-base-uncased` | Fast, lightweight | ~500MB | Very Fast |
| **medium** | `microsoft/DialoGPT-medium` | Balanced quality | ~1.5GB | Fast |
| **large** | `microsoft/DialoGPT-large` | Better quality | ~3GB | Medium |
| **best** | `microsoft/DialoGPT-xlarge` | Best quality | ~6GB | Slow |

## 🔧 Model Management

### Check Model Status

```bash
curl http://localhost:8080/api/local-models/status
```

### Get Model Information

```bash
curl http://localhost:8080/api/local-models/info
```

### Test Model

```bash
curl -X POST http://localhost:8080/api/local-models/test \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, how are you?"}'
```

## 🐛 Troubleshooting

### Common Issues

1. **Out of Memory Error**
   ```bash
   # Use smaller model
   export HF_MODEL_SIZE=small
   
   # Or increase JVM memory
   export MAVEN_OPTS="-Xmx4g"
   ```

2. **Model Download Fails**
   ```bash
   # Check internet connection
   # Clear cache and retry
   rm -rf ./models
   ./setup-local-models.sh
   ```

3. **Slow Performance**
   ```bash
   # Use smaller model
   export HF_MODEL_SIZE=small
   
   # Or enable GPU (if available)
   export LOCAL_MODEL_USE_GPU=true
   ```

4. **Java Version Issues**
   ```bash
   # Check Java version
   java -version
   
   # Should be 17 or higher
   ```

### Performance Optimization

1. **Use SSD Storage**: Store models on SSD for faster loading
2. **Increase Memory**: More RAM allows larger models
3. **Model Caching**: Keep frequently used models in memory
4. **Batch Processing**: Process multiple requests together

## 📊 Monitoring

### Memory Usage

```bash
curl http://localhost:8080/api/local-models/memory
```

### Model Cache

```bash
ls -la ./models/
du -sh ./models/
```

## 🔒 Security Considerations

- Models are cached locally - ensure disk space
- No data is sent to external APIs
- Consider encrypting model cache if needed
- Regular security updates for dependencies

## 🚀 Advanced Configuration

### Custom Models

To use custom Hugging Face models:

1. Update `LocalHuggingFaceService.java`
2. Add model to `RECOMMENDED_MODELS` map
3. Restart the application

### GPU Support

For GPU acceleration (if available):

```bash
export LOCAL_MODEL_USE_GPU=true
```

Note: Requires CUDA-compatible GPU and drivers.

### Multiple Model Instances

To run multiple models simultaneously:

```yaml
local-models:
  max-models-in-memory: 3
```

## 📈 Performance Tips

1. **Start with small models** for testing
2. **Monitor memory usage** during development
3. **Use model caching** for production
4. **Consider model quantization** for smaller models
5. **Batch requests** when possible

## 🆘 Support

If you encounter issues:

1. Check the logs: `tail -f backend/logs/application.log`
2. Verify Java version: `java -version`
3. Check memory: `free -h`
4. Test model loading: `curl http://localhost:8080/api/local-models/status`

## 📚 Additional Resources

- [Hugging Face Models](https://huggingface.co/models)
- [DJL Documentation](https://djl.ai/)
- [Spring Boot Configuration](https://spring.io/projects/spring-boot)

---

**Happy coding! 🎉** Your HR Chatbot now runs completely locally with no API dependencies!
