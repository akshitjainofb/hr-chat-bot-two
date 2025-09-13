# Hybrid Memory System - Quick Reference

## Key Classes

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `TokenLimiter` | Token-based sliding window | `trimToTokenLimit()`, `getTokenLimit()` |
| `Tokenizer` | Token counting | `countTokens()`, `estimateContextTokens()` |
| `ConversationMemory` | Memory container | `getCombinedContext()`, `getMemorySummary()` |
| `ConversationMemoryService` | Memory orchestration | `buildConversationMemory()`, `archiveToLongTermMemory()` |
| `PineconeService` | Vector storage | `indexConversationMemory()`, `searchConversationMemory()` |

## Configuration Properties

```yaml
memory:
  token-limit: 2000                    # Short-term memory limit
  long-term-memory-limit: 5           # Max long-term memory items
  document-context-limit: 5           # Max document context items
  archive-batch-size: 20              # Archive batch size
```

## Memory Flow

```
User Message
    ↓
Get all recent messages from DB
    ↓
Apply token-based sliding window
    ↓
Archive old messages to Pinecone
    ↓
Retrieve relevant long-term memory
    ↓
Retrieve HR document context
    ↓
Generate LLM response
    ↓
Save new messages to DB
```

## Memory Types

- **Short-term**: Recent messages from database (within token limit)
- **Long-term**: Older messages from Pinecone (semantic search)
- **Document**: HR documents from Pinecone (semantic search)

## Common Operations

### Get Memory for Chat
```java
ConversationMemory memory = conversationMemoryService
    .buildConversationMemory(chatRoom, userQuery, user);
```

### Check Token Usage
```java
int tokens = tokenizer.countTokens(text);
boolean fits = tokenLimiter.wouldFit(message, currentTokens);
```

### Archive Messages
```java
conversationMemoryService.archiveToLongTermMemory(chatRoom, user, messages);
```

### Clear Memory
```java
conversationMemoryService.clearConversationMemory(chatRoom, user);
```

## Debugging

### Enable Debug Logs
```yaml
logging:
  level:
    com.hrchatbot.service.ConversationMemoryService: DEBUG
    com.hrchatbot.util.TokenLimiter: DEBUG
```

### Memory Summary
```java
log.info("Memory: {}", memory.getMemorySummary());
// Output: "Memory: 15 short-term, 3 long-term, 2 docs, 1847 tokens"
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Memory not archived | Check Pinecone connection |
| Token count wrong | Adjust tokenizer settings |
| Slow performance | Reduce token limits |
| Memory not found | Check namespace permissions |

## API Endpoints (Unchanged)

- `POST /api/chat/send` - Send message (uses hybrid memory automatically)
- `GET /api/chat/rooms` - Get chat rooms
- `DELETE /api/chat/rooms/{id}` - Delete chat room (clears memory)
- `DELETE /api/chat/rooms/{id}/messages` - Clear messages (clears memory)
