# HR Chatbot System

A sophisticated HR Chatbot system built with Spring Boot and React, featuring AI-powered document search, multiple LLM providers, and a beautiful user interface.

## 🚀 Features

### Backend (Spring Boot)
- **Multiple LLM Support**: OpenAI, Google Gemini, Hugging Face
- **Vector Search**: Pinecone integration for semantic document search
- **PDF Processing**: Automatic text extraction and indexing
- **Authentication**: Google OAuth2 integration
- **Database**: MySQL with JPA/Hibernate
- **RESTful API**: Clean API design with proper error handling

### Frontend (React)
- **Modern UI**: Sophisticated design with TailwindCSS and Framer Motion
- **Real-time Chat**: Interactive chat interface with typing indicators
- **Document Management**: Drag-and-drop PDF upload with progress tracking
- **Settings Page**: LLM provider selection and user preferences
- **Responsive Design**: Works on desktop and mobile devices

### Key Capabilities
- 📄 **PDF-First Search**: Prioritizes answers from uploaded documents
- 🤖 **Multiple AI Models**: Switch between different LLM providers
- 💬 **Persistent Chat**: Conversation history per chat room
- 🔍 **Semantic Search**: Vector-based document search using Pinecone
- 🎨 **Elegant UI**: Production-ready, enterprise-grade interface
- ⚡ **Real-time**: Smooth animations and loading states

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React UI      │    │  Spring Boot    │    │     MySQL       │
│   (Frontend)    │◄──►│   (Backend)     │◄──►│   (Database)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │    Pinecone     │
                       │ (Vector Search) │
                       └─────────────────┘
```

## 🛠️ Tech Stack

### Backend
- **Java 17** with Spring Boot 3.2.0
- **Spring Security** with OAuth2
- **Spring Data JPA** with Hibernate
- **MySQL 8.0** database
- **Pinecone 5.1.0** for vector search
- **Apache PDFBox** for PDF processing
- **Google AI (Gemini) 0.6.0** for AI responses
- **Hugging Face API** for alternative AI models
- **Maven** for dependency management

### Frontend
- **React 18** with functional components
- **TailwindCSS** for styling
- **Framer Motion** for animations
- **Axios** for API calls
- **React Router** for navigation
- **React Hot Toast** for notifications

### Infrastructure
- **Docker** and Docker Compose
- **MySQL** for data persistence
- **Pinecone** for vector storage

## 🚀 Quick Start

### Prerequisites
- **Java 17+** - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
- **Node.js 18+** - Download from [nodejs.org](https://nodejs.org/)
- **Maven 3.6+** - Download from [maven.apache.org](https://maven.apache.org/download.cgi)
- **MySQL 8.0+** - Download from [mysql.com](https://dev.mysql.com/downloads/mysql/)
- **Pinecone account** - Sign up at [pinecone.io](https://www.pinecone.io/)
- **Google OAuth2 credentials** - Set up at [Google Cloud Console](https://console.cloud.google.com/)
- **API keys** for LLM providers (OpenAI, Gemini, Hugging Face)

### 1. Clone the Repository
```bash
git clone <repository-url>
cd hr-chat-bot-two
```

### 2. Set Up Environment Variables
```bash
cp env.example .env
# Edit .env with your API keys and configuration
```

### 3. Set Up MySQL Database
```bash
# Start MySQL service
# On macOS with Homebrew:
brew services start mysql

# On Ubuntu/Debian:
sudo systemctl start mysql

# On Windows:
# Start MySQL service from Services or MySQL Workbench

# Create database
mysql -u root -p
CREATE DATABASE hr_chatbot;
exit
```

### 4. Start the Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend will start on http://localhost:8080

### 5. Start the Frontend (in a new terminal)
```bash
cd frontend
npm install
npm start
```

The frontend will start on http://localhost:3000

### 6. Access the Application
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Database**: localhost:3306

## 🔧 Detailed Setup Instructions

### Step 1: Install Prerequisites

#### Java 17+
```bash
# Check if Java is installed
java -version

# If not installed, download from:
# https://www.oracle.com/java/technologies/downloads/
# or
# https://openjdk.org/
```

#### Node.js 18+
```bash
# Check if Node.js is installed
node --version
npm --version

# If not installed, download from:
# https://nodejs.org/
```

#### Maven 3.6+
```bash
# Check if Maven is installed
mvn --version

# If not installed, download from:
# https://maven.apache.org/download.cgi
```

#### MySQL 8.0+
```bash
# Check if MySQL is installed
mysql --version

# If not installed, download from:
# https://dev.mysql.com/downloads/mysql/
```

### Step 2: Database Setup
```bash
# Start MySQL service
# On macOS:
brew services start mysql

# On Ubuntu/Debian:
sudo systemctl start mysql

# On Windows:
# Start MySQL service from Services

# Create database and user
mysql -u root -p
CREATE DATABASE hr_chatbot;
CREATE USER 'hruser'@'localhost' IDENTIFIED BY 'hrpassword';
GRANT ALL PRIVILEGES ON hr_chatbot.* TO 'hruser'@'localhost';
FLUSH PRIVILEGES;
exit
```

### Step 3: API Keys Setup
1. **Google OAuth2**:
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project
   - Enable Google+ API
   - Create OAuth2 credentials
   - Add redirect URI: `http://localhost:8080/api/login/oauth2/code/google`

2. **OpenAI**:
   - Get API key from [OpenAI Platform](https://platform.openai.com/)

3. **Pinecone**:
   - Create account at [Pinecone](https://www.pinecone.io/)
   - Create index with dimension 1536 and metric cosine

4. **Update .env file**:
```bash
cp env.example .env
# Edit .env with your actual API keys
```

### Step 4: Backend Setup
```bash
# Option 1: Using the run script
./run-backend.sh

# Option 2: Manual setup
cd backend
mvn clean install
mvn spring-boot:run
```

### Step 5: Frontend Setup
```bash
# In a new terminal

# Option 1: Using the run script
./run-frontend.sh

# Option 2: Manual setup
cd frontend
npm install
npm start
```

### Quick Start (Alternative)
If you prefer to run everything manually:

**Terminal 1 - Backend:**
```bash
./run-backend.sh
```

**Terminal 2 - Frontend:**
```bash
./run-frontend.sh
```

## 📋 Configuration

### Required API Keys

1. **Google OAuth2**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create OAuth2 credentials
   - Add authorized redirect URI: `http://localhost:8080/api/login/oauth2/code/google`

2. **OpenAI**
   - Get API key from [OpenAI Platform](https://platform.openai.com/)
   - Add to `.env` file

3. **Google Gemini**
   - Get API key from [Google AI Studio](https://makersuite.google.com/)
   - Add to `.env` file

4. **Hugging Face**
   - Get API key from [Hugging Face](https://huggingface.co/)
   - Add to `.env` file

5. **Pinecone**
   - Create account at [Pinecone](https://www.pinecone.io/)
   - Create an index with dimension 1536
   - Add API key and environment to `.env` file

### Database Configuration
The application will automatically create the database schema on first run. Make sure MySQL is running and accessible.

## 📁 Project Structure

```
hr-chat-bot-two/
├── backend/
│   ├── src/main/java/com/hrchatbot/
│   │   ├── controller/          # REST controllers
│   │   ├── service/            # Business logic
│   │   ├── entity/             # JPA entities
│   │   ├── repository/         # Data access layer
│   │   ├── dto/                # Data transfer objects
│   │   └── config/             # Configuration classes
│   ├── src/main/resources/
│   │   └── application.yml     # Application configuration
│   └── pom.xml                 # Maven dependencies
├── frontend/
│   ├── src/
│   │   ├── components/         # React components
│   │   ├── pages/              # Page components
│   │   ├── contexts/           # React contexts
│   │   └── App.js              # Main app component
│   ├── package.json            # NPM dependencies
│   └── tailwind.config.js      # TailwindCSS config
├── database/
│   └── init.sql                # Database schema
├── docker-compose.yml          # Docker configuration
└── README.md                   # This file
```

## 🔌 API Endpoints

### Authentication
- `GET /api/auth/user` - Get current user
- `POST /api/auth/logout` - Logout user

### Chat
- `GET /api/chat/rooms` - Get user's chat rooms
- `POST /api/chat/rooms` - Create new chat room
- `GET /api/chat/rooms/{id}` - Get specific chat room
- `PUT /api/chat/rooms/{id}` - Update chat room
- `DELETE /api/chat/rooms/{id}` - Delete chat room
- `POST /api/chat/send` - Send message

### PDF Management
- `GET /api/pdf/documents` - Get user's documents
- `POST /api/pdf/upload` - Upload PDF document
- `DELETE /api/pdf/documents/{id}` - Delete document

### Settings
- `GET /api/settings/llm-providers` - Get available LLM providers
- `POST /api/settings/llm-provider` - Update preferred LLM provider

## 🎨 UI Features

### Chat Interface
- Real-time messaging with typing indicators
- Message history with context indicators
- Suggested follow-up questions
- Loading animations with playful steps
- Responsive design for all screen sizes

### Document Management
- Drag-and-drop PDF upload
- Document status tracking (indexed/processing)
- File size and upload date display
- Document preview and management

### Settings Page
- LLM provider selection with descriptions
- User profile information
- System status indicators
- Account management

## 🔒 Security Features

- Google OAuth2 authentication
- JWT token-based sessions
- CORS configuration
- Input validation and sanitization
- SQL injection prevention with JPA
- File upload security

## 🚀 Deployment

### Production Deployment
1. Set up production environment variables
2. Configure production database
3. Set up Pinecone production index
4. Deploy using Docker Compose or Kubernetes
5. Configure reverse proxy (Nginx)
6. Set up SSL certificates

### Environment Variables for Production
```bash
# Database
DB_USERNAME=production_user
DB_PASSWORD=secure_password
DB_URL=jdbc:mysql://production-db:3306/hr_chatbot

# OAuth2
GOOGLE_CLIENT_ID=production_client_id
GOOGLE_CLIENT_SECRET=production_client_secret

# API Keys
OPENAI_API_KEY=production_openai_key
GEMINI_API_KEY=production_gemini_key
HUGGINGFACE_API_KEY=production_hf_key
PINECONE_API_KEY=production_pinecone_key

# CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

## 🧪 Testing

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
```bash
cd frontend
npm test
```

## 📈 Performance Considerations

- **Database Indexing**: Optimized queries with proper indexes
- **Caching**: Consider adding Redis for session caching
- **CDN**: Use CDN for static assets in production
- **Load Balancing**: Multiple backend instances for high availability
- **Vector Search**: Pinecone handles vector operations efficiently

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review the API endpoints

## 🔮 Future Enhancements

- [ ] Voice input/output support
- [ ] Multi-language support
- [ ] Advanced analytics dashboard
- [ ] Integration with more document types
- [ ] Real-time collaboration features
- [ ] Mobile app development
- [ ] Advanced AI model fine-tuning
- [ ] Enterprise SSO integration

---

**Built with ❤️ for modern HR teams**
