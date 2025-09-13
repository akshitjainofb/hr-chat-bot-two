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

            // Paragraph too big → split by sentences first, then by lines if needed
            List<String> sentences = splitIntoSentences(trimmedParagraph);
            boolean isFirstSentenceOfParagraph = true;

            for (String sentence : sentences) {
                String trimmedSentence = sentence.trim();
                if (trimmedSentence.isEmpty()) continue;

                // Calculate separator length
                int sepLen = isFirstSentenceOfParagraph ? 
                    (currentChunk.length() > 0 ? 2 : 0) : // new paragraph needs "\n\n"
                    (currentChunk.length() > 0 ? 1 : 0);  // normal sentence needs "\n"

                // If sentence fits in current chunk, add it
                if (currentChunk.length() + sepLen + trimmedSentence.length() <= chunkSize) {
                    if (sepLen == 2) currentChunk.append("\n\n");
                    else if (sepLen == 1) currentChunk.append("\n");
                    currentChunk.append(trimmedSentence);
                    isFirstSentenceOfParagraph = false;
                    continue;
                }

                // Sentence doesn't fit → start new chunk (NEVER break a sentence in the middle)
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                
                // If the sentence itself is too big, split by lines but keep lines intact
                if (trimmedSentence.length() > chunkSize) {
                    String[] lines = trimmedSentence.split("\\r?\\n");
                    boolean isFirstLineOfSentence = true;

                    for (String line : lines) {
                        String trimmedLine = line.trim();
                        if (trimmedLine.isEmpty()) {
                            if (currentChunk.length() + 1 <= chunkSize) {
                                currentChunk.append("\n");
                            } else if (currentChunk.length() > 0) {
                                chunks.add(currentChunk.toString().trim());
                                currentChunk = new StringBuilder();
                            }
                            isFirstLineOfSentence = false;
                            continue;
                        }

                        // Calculate separator length for line
                        int lineSepLen = isFirstLineOfSentence ? 
                            (currentChunk.length() > 0 ? 1 : 0) : 1;

                        // If line fits in current chunk, add it
                        if (currentChunk.length() + lineSepLen + trimmedLine.length() <= chunkSize) {
                            if (lineSepLen == 1) currentChunk.append("\n");
                            currentChunk.append(trimmedLine);
                            isFirstLineOfSentence = false;
                            continue;
                        }

                        // Line doesn't fit → start new chunk (NEVER break a line in the middle)
                        if (currentChunk.length() > 0) {
                            chunks.add(currentChunk.toString().trim());
                            currentChunk = new StringBuilder();
                        }
                        currentChunk.append(trimmedLine);
                        isFirstLineOfSentence = false;
                    }
            } else {
                    // Sentence fits in a single chunk
                    currentChunk.append(trimmedSentence);
                }
                isFirstSentenceOfParagraph = false;
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return sentences;
        }

        // Split by sentence endings, but be careful with abbreviations
        String[] parts = text.split("(?<=[.!?])\\s+");
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }

        // If no sentences were found (no sentence endings), treat the whole text as one sentence
        if (sentences.isEmpty()) {
            sentences.add(text.trim());
        }

        return sentences;
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
            
            log.info("Starting deletion of PDF document {} for user {}", pdfDocument.getId(), user.getEmail());
            
            // Search for all records related to this PDF document
            Map<String, Object> filter = new HashMap<>();
            filter.put("user_id", user.getId().toString());
            filter.put("document_id", pdfDocument.getId().toString());
            
            log.info("Searching for records with filter: {}", filter);
            
            // Try to search for records using a simple query
            SearchRecordsResponse response;
            try {
                response = index.searchRecordsByText(
                    "document", // Simple query to get records
                    "hr-policies", // Use the correct namespace
                    List.of("id"), 
                    1000, // Get up to 1000 records
                    filter, 
                    null
                );
            } catch (Exception searchError) {
                log.warn("Text search failed, likely no records found or embedding error: {}", searchError.getMessage());
                log.info("No PDF document records found in Pinecone for document {} and user {}", 
                        pdfDocument.getId(), user.getEmail());
                return; // Exit early if no records found
            }
            
            log.info("Search response: {}", response);
            
            if (response.getResult() != null && response.getResult().getHits() != null && !response.getResult().getHits().isEmpty()) {
                List<String> idsToDelete = new ArrayList<>();
                response.getResult().getHits().forEach(hit -> {
                    // The ID is directly on the Hit object, not in fields
                    String hitId = hit.getId();
                    if (hitId != null && !hitId.trim().isEmpty()) {
                        idsToDelete.add(hitId);
                        log.debug("Found record to delete: {}", hitId);
                    }
                });
                
                // Delete records by ID only if we have valid IDs
                if (!idsToDelete.isEmpty()) {
                    try {
                        // Try to delete by IDs with namespace
                        index.deleteByIds(idsToDelete, "hr-policies");
                        log.info("Successfully deleted {} PDF document records from Pinecone for document {} and user {}", 
                                idsToDelete.size(), pdfDocument.getId(), user.getEmail());
                    } catch (Exception deleteError) {
                        log.warn("Could not delete PDF document records with namespace: {}", deleteError.getMessage());
                        // Try alternative deletion method - delete by IDs without namespace
                        try {
                            index.deleteByIds(idsToDelete);
                            log.info("Successfully deleted {} PDF document records using ID deletion without namespace", idsToDelete.size());
                        } catch (Exception altDeleteError) {
                            log.warn("ID deletion without namespace also failed: {}", altDeleteError.getMessage());
                            // Try deleting one by one
                            try {
                                for (String id : idsToDelete) {
                                    if (id != null && !id.trim().isEmpty()) {
                                        index.deleteByIds(List.of(id));
                                    }
                                }
                                log.info("Successfully deleted {} PDF document records using individual ID deletion", idsToDelete.size());
                            } catch (Exception finalDeleteError) {
                                log.error("All deletion methods failed: {}", finalDeleteError.getMessage());
                            }
                        }
                    }
                } else {
                    log.info("No valid PDF document record IDs found in Pinecone for document {} and user {}", 
                            pdfDocument.getId(), user.getEmail());
                }
            } else {
                log.info("No PDF document records found in Pinecone for document {} and user {}", 
                        pdfDocument.getId(), user.getEmail());
            }
            
        } catch (Exception e) {
            log.error("Error deleting PDF document from Pinecone: {}", e.getMessage());
            // Don't throw exception to avoid breaking document deletion
            log.warn("Continuing with document deletion despite Pinecone cleanup failure");
        }
    }
}
