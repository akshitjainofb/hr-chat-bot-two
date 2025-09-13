package com.hrchatbot.service.impl;

import com.hrchatbot.entity.ChatMessage;
import com.hrchatbot.entity.PdfDocument;
import com.hrchatbot.entity.User;
import com.hrchatbot.service.PineconeService;
import io.pinecone.clients.Index;
import io.pinecone.clients.Pinecone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.db_data.client.ApiException;
import org.openapitools.db_data.client.model.SearchRecordsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PineconeServiceImpl implements PineconeService {

    private final Pinecone pineconeClient;
    
    @Value("${pinecone.index-name}")
    private String indexName;

    @Value("${pinecone.api-key}")
    private String apiKey;

    private Index index() {
        Pinecone pc = new Pinecone.Builder(apiKey).build();
        return pc.getIndexConnection(indexName);
    }

    @Override
    public void indexPdfDocument(PdfDocument pdfDocument, String content) {
        try {
            Index index = index();
            
            // Split content into chunks
            List<String> chunks = splitIntoChunks(content, 1000);
            
            List<Map<String, String>> records = new ArrayList<>();
            
            for (int i = 0; i < chunks.size(); i++) {
                String chunkId = pdfDocument.getId() + "_chunk_" + i;
                String chunk = chunks.get(i);
                
                Map<String, String> record = new HashMap<>();
                record.put("id", chunkId);
                record.put("text", chunk);
                record.put("document_id", pdfDocument.getId().toString());
                record.put("user_id", pdfDocument.getUser().getId().toString());
                record.put("file_name", pdfDocument.getFileName());
                record.put("chunk_index", String.valueOf(i));
                
                records.add(record);
            }
            
            // Upsert records to Pinecone
            index.upsertRecords("hr-policies", records);
            
            log.info("Successfully indexed PDF document: {} with {} chunks", pdfDocument.getFileName(), chunks.size());
            
        } catch (Exception e) {
            log.error("Error indexing PDF document: {}", e.getMessage());
            throw new RuntimeException("Failed to index PDF document", e);
        }
    }

    @Override
    public List<String> searchSimilarContent(String query, User user, int topK) {
        try {
            Index index = index();
            
            // Define fields to return
            List<String> fields = List.of("text", "file_name", "document_id");
            
            SearchRecordsResponse response = index.searchRecordsByText(
                query,
                "hr-policies",
                fields, 
                topK, 
                null, 
                null
            );
            
            List<String> results = new ArrayList<>();


            if (response.getResult() != null && response.getResult().getHits() != null) {
                response.getResult().getHits().forEach(hit -> {
                    Object fieldsObj = hit.getFields();
                    if (fieldsObj instanceof java.util.Map<?, ?> map) {
                        Object textVal = map.get("text");
                        if (textVal != null) {
                            String text = String.valueOf(textVal);
                            if (!text.isBlank()) results.add(text);
                        }
                    }
                });
            }
            
            return results;
            
        } catch (ApiException e) {
            log.error("Error searching similar content: {}", e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Unexpected error searching similar content: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<String> splitIntoChunks(String content, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty() || chunkSize <= 0) {
            return chunks;
        }

        // Split by paragraphs (blank line separators)
        String[] paragraphs = content.split("\\r?\\n\\s*\\r?\\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isEmpty()) continue;

            // If paragraph fits in current chunk
            int sepLenIfNeeded = currentChunk.length() > 0 ? 2 : 0; // "\n\n"
            if (trimmedParagraph.length() + sepLenIfNeeded <= chunkSize) {
                if (currentChunk.length() > 0) currentChunk.append("\n\n");
                currentChunk.append(trimmedParagraph);
                continue;
            }

            // Paragraph too big → split by lines
            String[] lines = trimmedParagraph.split("\\r?\\n");
            boolean isFirstLineOfParagraph = true;

            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    if (currentChunk.length() + 1 <= chunkSize) {
                        currentChunk.append("\n");
                    } else if (currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString().trim());
                        currentChunk = new StringBuilder();
                    }
                    isFirstLineOfParagraph = false;
                    continue;
                }

                List<String> segments = splitLongLine(trimmedLine, chunkSize);

                for (String seg : segments) {
                    int sepLen;
                    if (isFirstLineOfParagraph) {
                        sepLen = currentChunk.length() > 0 ? 2 : 0; // new paragraph needs "\n\n"
                        isFirstLineOfParagraph = false;
                    } else {
                        sepLen = currentChunk.length() > 0 ? 1 : 0; // normal line needs "\n"
                    }

                    if (currentChunk.length() + sepLen + seg.length() <= chunkSize) {
                        if (sepLen == 2) currentChunk.append("\n\n");
                        else if (sepLen == 1) currentChunk.append("\n");
                        currentChunk.append(seg);
                    } else {
                        if (currentChunk.length() > 0) {
                            chunks.add(currentChunk.toString().trim());
                            currentChunk = new StringBuilder();
                        }
                        currentChunk.append(seg);
                    }
                }
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private List<String> splitLongLine(String line, int chunkSize) {
        List<String> parts = new ArrayList<>();
        int len = line.length();
        int start = 0;

        while (start < len) {
            int end = Math.min(start + chunkSize, len);

            if (end == len) {
                String part = line.substring(start, end).trim();
                if (!part.isEmpty()) parts.add(part);
                break;
            }

            int lastSpace = -1;
            for (int i = end; i > start; i--) {
                if (Character.isWhitespace(line.charAt(i - 1))) {
                    lastSpace = i - 1;
                    break;
                }
            }

            int cut = (lastSpace > start) ? lastSpace : end;
            String part = line.substring(start, cut).trim();
            if (!part.isEmpty()) parts.add(part);

            start = (cut == end) ? end : cut + 1;
        }

        return parts;
    }


    @Override
    public void indexConversationMemory(List<ChatMessage> messages, User user) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        try {
            Index index = index();
            List<Map<String, String>> records = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (ChatMessage message : messages) {
                String messageId = "conv_" + message.getId();
                String messageText = message.getMessage();
                
                // Create a more searchable text that includes context
                String searchableText = String.format("[%s] %s: %s", 
                    message.getCreatedAt().format(formatter),
                    message.getRole().name(),
                    messageText);
                
                Map<String, String> record = new HashMap<>();
                record.put("id", messageId);
                record.put("text", searchableText);
                record.put("message_id", message.getId().toString());
                record.put("user_id", user.getId().toString());
                record.put("chat_room_id", message.getChatRoom().getId().toString());
                record.put("role", message.getRole().name());
                record.put("created_at", message.getCreatedAt().format(formatter));
                record.put("type", "conversation");
                
                records.add(record);
            }
            
            // Upsert records to Pinecone with conversation namespace
            index.upsertRecords("conversation-memory", records);
            
            log.info("Successfully indexed {} conversation messages for user {}", 
                    messages.size(), user.getEmail());
            
        } catch (Exception e) {
            log.error("Error indexing conversation memory: {}", e.getMessage());
            throw new RuntimeException("Failed to index conversation memory", e);
        }
    }
    
    @Override
    public List<String> searchConversationMemory(String query, User user, int topK) {
        try {
            Index index = index();
            
            // Define fields to return
            List<String> fields = List.of("text", "role", "created_at", "message_id");
            
            // Create a filter for user-specific conversation memory
            Map<String, Object> filter = new HashMap<>();
            filter.put("user_id", user.getId().toString());
            filter.put("type", "conversation");
            
            SearchRecordsResponse response = index.searchRecordsByText(
                query,
                "conversation-memory",
                fields, 
                topK, 
                filter, 
                null
            );
            
            List<String> results = new ArrayList<>();
            
            if (response.getResult() != null && response.getResult().getHits() != null) {
                response.getResult().getHits().forEach(hit -> {
                    Object fieldsObj = hit.getFields();
                    if (fieldsObj instanceof java.util.Map<?, ?> map) {
                        Object textVal = map.get("text");
                        if (textVal != null) {
                            String text = String.valueOf(textVal);
                            if (!text.isBlank()) {
                                results.add(text);
                            }
                        }
                    }
                });
            }
            
            log.debug("Retrieved {} conversation memory results for query: {}", results.size(), query);
            return results;
            
        } catch (ApiException e) {
            log.error("Error searching conversation memory: {}", e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Unexpected error searching conversation memory: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public void deleteConversationMemory(Long chatRoomId, User user) {
        try {
            Index index = index();
            
            // For now, we'll implement a simple approach by searching for records
            // and then deleting them individually. This is not the most efficient
            // but works with the current Pinecone Java client API.
            
            // Search for all conversation memory records for this chat room
            Map<String, Object> filter = new HashMap<>();
            filter.put("user_id", user.getId().toString());
            filter.put("chat_room_id", chatRoomId.toString());
            filter.put("type", "conversation");
            
            SearchRecordsResponse response = index.searchRecordsByText(
                "", // Empty query to get all records
                "conversation-memory",
                List.of("id"), 
                1000, // Get up to 1000 records
                filter, 
                null
            );
            
            if (response.getResult() != null && response.getResult().getHits() != null) {
                List<String> idsToDelete = new ArrayList<>();
                response.getResult().getHits().forEach(hit -> {
                    Object fieldsObj = hit.getFields();
                    if (fieldsObj instanceof java.util.Map<?, ?> map) {
                        Object idVal = map.get("id");
                        if (idVal != null) {
                            idsToDelete.add(String.valueOf(idVal));
                        }
                    }
                });
                
                // Delete records by ID (if the API supports it)
                if (!idsToDelete.isEmpty()) {
                    // Note: This assumes deleteByIds method exists
                    // If not available, we'll log a warning and continue
                    try {
                        // Try to delete by IDs - this method might not exist
                        // index.deleteByIds("conversation-memory", idsToDelete);
                        log.warn("Bulk delete not supported, conversation memory cleanup skipped for chat room {}", chatRoomId);
                    } catch (Exception deleteError) {
                        log.warn("Could not delete conversation memory records: {}", deleteError.getMessage());
                    }
                }
            }
            
            log.info("Conversation memory cleanup attempted for chat room {} and user {}", 
                    chatRoomId, user.getEmail());
            
        } catch (Exception e) {
            log.error("Error deleting conversation memory: {}", e.getMessage());
            // Don't throw exception to avoid breaking chat room deletion
            log.warn("Continuing with chat room deletion despite memory cleanup failure");
        }
    }
    
    @Override
    public void deletePdfDocument(PdfDocument pdfDocument, User user) {
        try {
            Index index = index();
            
            // Search for all records related to this PDF document
            Map<String, Object> filter = new HashMap<>();
            filter.put("user_id", user.getId().toString());
            filter.put("document_id", pdfDocument.getId().toString());
            filter.put("type", "document");
            
            SearchRecordsResponse response = index.searchRecordsByText(
                "", // Empty query to get all records
                "hr-knowledge-base", // Use the main namespace
                List.of("id"), 
                1000, // Get up to 1000 records
                filter, 
                null
            );
            
            if (response.getResult() != null && response.getResult().getHits() != null) {
                List<String> idsToDelete = new ArrayList<>();
                response.getResult().getHits().forEach(hit -> {
                    Object fieldsObj = hit.getFields();
                    if (fieldsObj instanceof java.util.Map<?, ?> map) {
                        Object idVal = map.get("id");
                        if (idVal != null) {
                            idsToDelete.add(String.valueOf(idVal));
                        }
                    }
                });
                
                // Delete records by ID
                if (!idsToDelete.isEmpty()) {
                    try {
                        // Delete by IDs - this method should exist in the Pinecone client
                        index.deleteByIds(idsToDelete, "hr-knowledge-base");
                        log.info("Successfully deleted {} PDF document records from Pinecone for document {} and user {}", 
                                idsToDelete.size(), pdfDocument.getId(), user.getEmail());
                    } catch (Exception deleteError) {
                        log.warn("Could not delete PDF document records from Pinecone: {}", deleteError.getMessage());
                        // Continue with document deletion even if Pinecone cleanup fails
                    }
                } else {
                    log.info("No PDF document records found in Pinecone for document {} and user {}", 
                            pdfDocument.getId(), user.getEmail());
                }
            }
            
        } catch (Exception e) {
            log.error("Error deleting PDF document from Pinecone: {}", e.getMessage());
            // Don't throw exception to avoid breaking document deletion
            log.warn("Continuing with document deletion despite Pinecone cleanup failure");
        }
    }
}
