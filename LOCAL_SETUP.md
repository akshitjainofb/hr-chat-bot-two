# HR Chatbot - Local Development Setup

## 🚀 Quick Start (5 Minutes)

### Prerequisites Check
```bash
# Check if you have the required software
java -version    # Should be 17+
node --version  # Should be 18+
mvn --version   # Should be 3.6+
mysql --version # Should be 8.0+
```

### 1. Clone and Setup
```bash
git clone <your-repo-url>
cd hr-chat-bot-two
cp env.example .env
# Edit .env with your API keys
```

### 2. Database Setup
```bash
# Start MySQL
brew services start mysql  # macOS
# or
sudo systemctl start mysql  # Linux

# Create database
mysql -u root -p
CREATE DATABASE hr_chatbot;
CREATE USER 'hruser'@'localhost' IDENTIFIED BY 'hrpassword';
GRANT ALL PRIVILEGES ON hr_chatbot.* TO 'hruser'@'localhost';
FLUSH PRIVILEGES;
exit
```

### 3. Start the Application

**Terminal 1 - Backend:**
```bash
./run-backend.sh
```

**Terminal 2 - Frontend:**
```bash
./run-frontend.sh
```

### 4. Access the Application
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api

## 🔑 Required API Keys

### 1. Google OAuth2
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create project → Enable Google+ API
3. Create OAuth2 credentials
4. Add redirect URI: `http://localhost:8080/api/login/oauth2/code/google`
5. Copy Client ID and Secret to `.env`

### 2. OpenAI
1. Get API key from [OpenAI Platform](https://platform.openai.com/)
2. Add to `.env` file

### 3. Pinecone
1. Create account at [Pinecone](https://www.pinecone.io/)
2. Create index (dimension: 1536, metric: cosine)
3. Copy API key and index name to `.env`

## 📁 Project Structure
```
hr-chat-bot-two/
├── backend/              # Spring Boot application
│   ├── src/main/java/   # Java source code
│   └── pom.xml          # Maven dependencies
├── frontend/            # React application
│   ├── src/            # React source code
│   └── package.json    # NPM dependencies
├── database/           # SQL schema
├── run-backend.sh     # Backend startup script
├── run-frontend.sh    # Frontend startup script
├── start.sh           # Setup guide
└── .env               # Environment variables
```

## 🛠️ Development Commands

### Backend Development
```bash
cd backend
mvn clean install      # Install dependencies
mvn spring-boot:run    # Start backend
mvn test              # Run tests
```

### Frontend Development
```bash
cd frontend
npm install           # Install dependencies
npm start            # Start frontend
npm test             # Run tests
npm run build        # Build for production
```

## 🔧 Troubleshooting

### Common Issues

1. **Port 8080 already in use**
   ```bash
   # Find and kill the process
   lsof -ti:8080 | xargs kill -9
   ```

2. **Port 3000 already in use**
   ```bash
   # Find and kill the process
   lsof -ti:3000 | xargs kill -9
   ```

3. **MySQL connection failed**
   ```bash
   # Check if MySQL is running
   brew services list | grep mysql
   # or
   sudo systemctl status mysql
   ```

4. **Java version issues**
   ```bash
   # Check Java version
   java -version
   # Install Java 17+ if needed
   ```

5. **Node.js version issues**
   ```bash
   # Check Node version
   node --version
   # Install Node 18+ if needed
   ```

### Reset Everything
```bash
# Stop all processes (Ctrl+C in both terminals)
# Clear caches
cd backend && mvn clean
cd frontend && rm -rf node_modules package-lock.json
# Restart MySQL
brew services restart mysql
# Start fresh
./run-backend.sh    # Terminal 1
./run-frontend.sh   # Terminal 2
```

## 📊 Features Overview

### ✅ Implemented Features
- **Authentication**: Google OAuth2 login
- **Chat Interface**: Real-time messaging with AI
- **PDF Upload**: Drag-and-drop document management
- **Vector Search**: Pinecone 5.1.0 powered semantic search
- **Multiple LLMs**: OpenAI, Google Gemini 0.6.0, Hugging Face API
- **Settings**: LLM provider selection
- **Responsive UI**: Works on all devices

### 🎨 UI Highlights
- Modern, elegant design
- Smooth animations with Framer Motion
- Loading states with playful messages
- Drag-and-drop file upload
- Real-time chat interface
- Mobile-responsive design

## 🚀 Production Deployment

For production:
1. Use production database
2. Set up SSL certificates
3. Configure production OAuth2
4. Use production Pinecone index
5. Set up reverse proxy (Nginx)
6. Configure CORS for production domain

## 📞 Support

If you need help:
1. Check the console logs
2. Verify all API keys are correct
3. Ensure all prerequisites are installed
4. Review the troubleshooting section
5. Check the main README.md for detailed instructions

---

**Happy coding! 🎉**
