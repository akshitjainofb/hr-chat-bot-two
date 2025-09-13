# Hybrid Memory Architecture for HR Chatbot

## Overview

The HR Chatbot backend has been refactored to implement a sophisticated hybrid memory approach that combines short-term database storage with long-term Pinecone vector storage. This architecture provides intelligent conversation context management while maintaining performance and scalability.

## Architecture Components

### 1. TokenLimiter (`com.hrchatbot.util.TokenLimiter`)

**Purpose**: Manages conversation history using a sliding window based on token count rather than message count.

**Key Features**:
- Configurable token limits (default: 2000 tokens)
- Intelligent trimming that preserves the most recent messages
- Buffer tokens for system prompts (100 tokens)
- Integration with custom Tokenizer for accurate token counting

**Usage**:
```java
@Autowired
private TokenLimiter tokenLimiter;

List<ChatMessage> trimmedMessages = tokenLimiter.trimToTokenLimit(allMessages);
```

### 2. Tokenizer (`com.hrchatbot.util.Tokenizer`)

**Purpose**: Provides accurate token counting for various text inputs.

**Key Features**:
- Multiple approximation methods for token counting
- Word-based, character-based, and simple whitespace splitting
- Context-aware token estimation including system prompts
- Production-ready fallback mechanisms

**Usage**:
```java
@Autowired
private Tokenizer tokenizer;

int tokenCount = tokenizer.countTokens("Your text here");
```

### 3. ConversationMemory DTO (`com.hrchatbot.dto.ConversationMemory`)

**Purpose**: Encapsulates all memory components in a single, manageable object.

**Components**:
- `shortTermMemory`: Recent messages from database (within token limit)
- `longTermMemory`: Relevant older messages from Pinecone
- `documentContext`: HR document context from Pinecone
- `totalTokenCount`: Total estimated token usage

**Key Methods**:
- `getCombinedContext()`: Merges all memory into a single context string
- `getFormattedConversationHistory()`: Formats conversation for LLM input
- `getMemorySummary()`: Provides logging-friendly summary

### 4. ConversationMemoryService (`com.hrchatbot.service.ConversationMemoryService`)

**Purpose**: Orchestrates the hybrid memory approach, managing both short-term and long-term memory.

**Key Responsibilities**:
- Building conversation memory for each chat request
- Archiving old messages to long-term memory
- Coordinating between database and Pinecone
- Managing memory cleanup operations

**Main Method**:
```java
ConversationMemory buildConversationMemory(ChatRoom chatRoom, String userQuery, User user)
```

### 5. Enhanced PineconeService

**New Methods**:
- `indexConversationMemory()`: Stores chat messages in Pinecone with metadata
- `searchConversationMemory()`: Retrieves relevant conversation history
- `deleteConversationMemory()`: Cleans up conversation memory (with fallback)

**Namespaces**:
- `hr-policies`: HR document chunks
- `conversation-memory`: Chat message embeddings

### 6. Updated LlmService

**New Method**:
```java
ChatResponse generateResponseWithMemory(String userMessage, 
                                      ConversationMemory conversationMemory, 
                                      String provider, User user)
```

**Features**:
- Seamless integration with existing LLM providers
- Automatic fallback mechanisms
- Memory-aware response generation

## Memory Flow

### 1. Message Processing Flow

```
User Message → ChatServiceImpl.sendMessage()
    ↓
ConversationMemoryService.buildConversationMemory()
    ↓
├── Get all recent messages from database
├── Apply token-based sliding window (TokenLimiter)
├── Archive old messages to Pinecone
├── Retrieve relevant long-term memory from Pinecone
├── Retrieve HR document context from Pinecone
└── Build ConversationMemory object
    ↓
LlmService.generateResponseWithMemory()
    ↓
Generate response using combined context
    ↓
Save user and assistant messages to database
```

### 2. Memory Types

#### Short-term Memory (Database)
- **Source**: Recent messages from `chat_messages` table
- **Selection**: Token-based sliding window (most recent messages within limit)
- **Purpose**: Immediate conversation context
- **Performance**: Fast database queries

#### Long-term Memory (Pinecone)
- **Source**: Older messages archived to Pinecone
- **Selection**: Semantic similarity search based on current query
- **Purpose**: Relevant historical context
- **Performance**: Vector similarity search

#### Document Context (Pinecone)
- **Source**: HR policy documents and PDFs
- **Selection**: Semantic similarity search based on current query
- **Purpose**: Factual information and policy references
- **Performance**: Vector similarity search

## Configuration

### Application Properties

```yaml
# Memory Configuration
memory:
  token-limit: ${MEMORY_TOKEN_LIMIT:2000}           # Short-term memory token limit
  long-term-memory-limit: ${MEMORY_LONG_TERM_LIMIT:5}    # Max long-term memory items
  document-context-limit: ${MEMORY_DOCUMENT_LIMIT:5}      # Max document context items
  archive-batch-size: ${MEMORY_ARCHIVE_BATCH_SIZE:20}     # Batch size for archiving
```

### Environment Variables

```bash
MEMORY_TOKEN_LIMIT=2000
MEMORY_LONG_TERM_LIMIT=5
MEMORY_DOCUMENT_LIMIT=5
MEMORY_ARCHIVE_BATCH_SIZE=20
```

## Benefits

### 1. Intelligent Context Management
- **Token-aware**: Respects LLM context limits while maximizing relevant information
- **Semantic retrieval**: Finds relevant historical context based on meaning, not just recency
- **Balanced approach**: Combines immediate context with long-term memory

### 2. Performance Optimization
- **Efficient queries**: Database for recent messages, Pinecone for semantic search
- **Configurable limits**: Adjustable based on model capabilities and performance requirements
- **Batch processing**: Efficient archiving of old messages

### 3. Scalability
- **Horizontal scaling**: Pinecone handles large-scale vector operations
- **Memory efficiency**: Only loads relevant context, not entire conversation history
- **Configurable limits**: Easy to adjust based on usage patterns

### 4. Maintainability
- **Separation of concerns**: Clear boundaries between different memory types
- **Clean interfaces**: Well-defined service contracts
- **Comprehensive logging**: Detailed memory usage tracking

## Usage Examples

### Basic Usage

The hybrid memory system is automatically used when processing chat messages. No changes are required to existing API calls.

```java
// Existing API call works unchanged
ChatResponse response = chatService.sendMessage(request, user);
```

### Advanced Configuration

```java
// Custom token limit
@Value("${memory.token-limit:1500}")
private int customTokenLimit;

// Custom memory limits
@Value("${memory.long-term-memory-limit:10}")
private int customLongTermLimit;
```

### Memory Monitoring

```java
// Log memory usage
ConversationMemory memory = conversationMemoryService.buildConversationMemory(chatRoom, query, user);
log.info("Memory usage: {}", memory.getMemorySummary());
```

## Migration Notes

### Backward Compatibility
- All existing API endpoints remain unchanged
- Existing chat functionality continues to work
- No database schema changes required

### New Dependencies
- No new external dependencies added
- Uses existing Pinecone and Spring Boot infrastructure
- Leverages existing Lombok and Spring annotations

### Performance Impact
- **Positive**: More efficient context management
- **Neutral**: Similar response times with better context quality
- **Configurable**: Can be tuned based on performance requirements

## Troubleshooting

### Common Issues

1. **Memory not being archived**
   - Check Pinecone connection and API key
   - Verify namespace permissions
   - Review batch size configuration

2. **Token count inaccuracies**
   - Adjust tokenizer approximation methods
   - Consider using external tokenizer service
   - Monitor actual vs estimated token usage

3. **Performance issues**
   - Reduce token limits
   - Adjust batch sizes
   - Monitor Pinecone query performance

### Debugging

Enable debug logging:
```yaml
logging:
  level:
    com.hrchatbot.service.ConversationMemoryService: DEBUG
    com.hrchatbot.util.TokenLimiter: DEBUG
```

## Future Enhancements

### Potential Improvements
1. **Advanced Tokenization**: Integration with OpenAI's tiktoken or similar
2. **Memory Compression**: Summarization of old conversations
3. **Smart Archiving**: ML-based selection of messages to archive
4. **Memory Analytics**: Usage patterns and optimization insights
5. **Multi-tenant Memory**: User-specific memory isolation

### Monitoring and Metrics
1. **Token Usage Tracking**: Monitor actual vs estimated token counts
2. **Memory Hit Rates**: Track effectiveness of long-term memory retrieval
3. **Performance Metrics**: Response times and memory operation costs
4. **Quality Metrics**: User satisfaction with context relevance

## Conclusion

The hybrid memory architecture provides a robust, scalable solution for managing conversation context in the HR Chatbot. It balances performance, accuracy, and maintainability while providing a foundation for future enhancements.

The system is production-ready and can be deployed with minimal configuration changes. All existing functionality is preserved while adding sophisticated memory management capabilities.
