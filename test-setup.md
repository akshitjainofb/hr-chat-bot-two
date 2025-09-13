# HR Chatbot - Local Setup Verification

## Quick Test Checklist

### 1. Prerequisites Check
- [ ] Java 17+ installed (`java -version`)
- [ ] Node.js 18+ installed (`node --version`)
- [ ] Maven 3.6+ installed (`mvn --version`)
- [ ] MySQL 8.0+ installed and running (`mysql --version`)

### 2. Environment Setup
- [ ] Copy `env.example` to `.env`
- [ ] Configure all required API keys in `.env`
- [ ] MySQL database created and accessible

### 3. API Keys Required
- [ ] Google OAuth2 Client ID and Secret
- [ ] OpenAI API Key
- [ ] Google Gemini API Key (optional)
- [ ] Hugging Face API Key (optional)
- [ ] Pinecone API Key and Environment

### 4. Google OAuth2 Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Google+ API
4. Create OAuth2 credentials
5. Add authorized redirect URI: `http://localhost:8080/api/login/oauth2/code/google`
6. Copy Client ID and Secret to `.env`

### 5. Pinecone Setup
1. Create account at [Pinecone](https://www.pinecone.io/)
2. Create a new index with:
   - Dimension: 1536
   - Metric: cosine
   - Name: hr-chatbot-index (or update in .env)
3. Copy API key and environment to `.env`

### 6. Database Setup
```bash
# Start MySQL
# On macOS: brew services start mysql
# On Ubuntu: sudo systemctl start mysql
# On Windows: Start MySQL service

# Create database
mysql -u root -p
CREATE DATABASE hr_chatbot;
CREATE USER 'hruser'@'localhost' IDENTIFIED BY 'hrpassword';
GRANT ALL PRIVILEGES ON hr_chatbot.* TO 'hruser'@'localhost';
FLUSH PRIVILEGES;
exit
```

### 7. Start the Application

#### Terminal 1 - Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

#### Terminal 2 - Frontend
```bash
cd frontend
npm install
npm start
```

### 8. Verify Services
- [ ] Backend running at http://localhost:8080
- [ ] Frontend running at http://localhost:3000
- [ ] Database accessible on localhost:3306
- [ ] No error messages in console

### 9. Test Features
- [ ] Google OAuth2 login works
- [ ] Can create chat rooms
- [ ] Can send messages
- [ ] Can upload PDF documents
- [ ] Settings page loads
- [ ] LLM provider selection works

## Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 3000, 8080, and 3306 are available
2. **Java version**: Make sure you have Java 17+ installed
3. **Node.js version**: Ensure Node.js 18+ is installed
4. **Database connection**: Check MySQL is running and credentials are correct
5. **OAuth2 errors**: Verify redirect URI matches exactly
6. **Pinecone errors**: Check API key and index name are correct
7. **Maven issues**: Ensure Maven is installed and in PATH

### Backend Logs
```bash
# Check backend logs in the terminal where you ran mvn spring-boot:run
# Look for any error messages or exceptions
```

### Frontend Logs
```bash
# Check frontend logs in the terminal where you ran npm start
# Look for any compilation errors or network issues
```

### Database Connection Test
```bash
# Test MySQL connection
mysql -u hruser -p hr_chatbot
# Should connect successfully
```

### Reset Everything
```bash
# Stop both terminals (Ctrl+C)
# Restart MySQL if needed
# Clear any cached files
cd backend && mvn clean
cd frontend && rm -rf node_modules package-lock.json && npm install
```

## Production Deployment

For production deployment:
1. Use production database (not local MySQL)
2. Set up proper SSL certificates
3. Configure production OAuth2 credentials
4. Use production Pinecone index
5. Set up reverse proxy (Nginx)
6. Configure proper CORS origins
7. Use environment-specific configuration

## Support

If you encounter issues:
1. Check the console logs first
2. Verify all API keys are correct
3. Ensure all services are running
4. Check network connectivity
5. Review the README.md for detailed instructions
6. Make sure all prerequisites are installed correctly
