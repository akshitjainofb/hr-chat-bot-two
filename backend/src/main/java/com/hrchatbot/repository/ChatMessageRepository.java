package com.hrchatbot.repository;

import com.hrchatbot.entity.ChatMessage;
import com.hrchatbot.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom = :chatRoom ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessagesByChatRoom(@Param("chatRoom") ChatRoom chatRoom);
    
    @Query(value = "SELECT * FROM chat_messages WHERE chat_room_id = :chatRoomId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<ChatMessage> findRecentMessagesByChatRoomId(@Param("chatRoomId") Long chatRoomId, @Param("limit") int limit);
    
    void deleteByChatRoom(ChatRoom chatRoom);
}
