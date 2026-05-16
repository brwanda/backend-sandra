package com.earacg.earaconnect.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.earacg.earaconnect.model.Document;

@Repository
public interface DocumentRepo extends JpaRepository<Document, Long>{
    Optional<Document> findByStoredFilename(String storedFilename);
    
    // Find by original filename
    List<Document> findByOriginalFilename(String originalFilename);
    
    // Find by content type
    List<Document> findByContentType(String contentType);
    
    // Find documents uploaded after a certain date
    List<Document> findByUploadDateAfter(LocalDateTime date);
    
    // Find documents uploaded before a certain date
    List<Document> findByUploadDateBefore(LocalDateTime date);
    
    // Find documents by file size range
    List<Document> findByFileSizeBetween(Long minSize, Long maxSize);
    
    // Find documents larger than specified size
    List<Document> findByFileSizeGreaterThan(Long size);
    
    // Check if stored filename exists
    boolean existsByStoredFilename(String storedFilename);
}
