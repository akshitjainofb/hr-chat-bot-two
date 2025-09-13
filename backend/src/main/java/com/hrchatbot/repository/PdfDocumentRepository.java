package com.hrchatbot.repository;

import com.hrchatbot.entity.PdfDocument;
import com.hrchatbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long> {
    List<PdfDocument> findByUserOrderByCreatedAtDesc(User user);
    List<PdfDocument> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PdfDocument> findByUserAndIndexedTrueOrderByCreatedAtDesc(User user);
    List<PdfDocument> findByUserIdAndIndexedTrueOrderByCreatedAtDesc(Long userId);
}
