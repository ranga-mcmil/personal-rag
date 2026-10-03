package com.example.secondbrain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.secondbrain.entity.DocumentChunk;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(Long documentId);

    @Query("SELECT c FROM DocumentChunk c " +
            "WHERE c.document.owner.username = :username " +
            "AND (c.embedded IS NULL OR c.embedded = false)")
    List<DocumentChunk> findPendingByOwner(String username);
}
