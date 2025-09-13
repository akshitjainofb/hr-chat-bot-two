package com.hrchatbot.repository;

import com.hrchatbot.entity.ChatRoom;
import com.hrchatbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    List<ChatRoom> findByUserOrderByCreatedAtDesc(User user);
    List<ChatRoom> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserAndName(User user, String name);
}
