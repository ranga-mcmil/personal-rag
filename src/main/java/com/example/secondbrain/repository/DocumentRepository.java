package com.example.secondbrain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.secondbrain.entity.Document;

public interface DocumentRepository extends JpaRepository<Document, Long>{
    List<Document> findByOwnerUsername(String username);
}
