package com.example.secondbrain.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.secondbrain.entity.Document;
import com.example.secondbrain.entity.DocumentChunk;
import com.example.secondbrain.entity.User;
import com.example.secondbrain.repository.DocumentChunkRepository;
import com.example.secondbrain.repository.DocumentRepository;
import com.example.secondbrain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@RequiredArgsConstructor 
@Slf4j 
public class DocumentIngestionService {

    private final TextExtractionService textExtractionService;
    private final ChunkingService chunkingService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final UserRepository userRepository;    

    public Document ingest(MultipartFile file, String username) throws IOException {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String text = textExtractionService.extractText(file.getOriginalFilename(), file.getBytes());
        List<String> chunks = chunkingService.chunk(text);

        Document document = new Document();
        document.setFilename(file.getOriginalFilename());
        document.setSourceType(file.getContentType());
        document.setUploadedAt(Instant.now());
        document.setOwner(owner);
        documentRepository.save(document);

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(document);
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            documentChunkRepository.save(chunk);
        }
        
        log.info("Ingested '{}' into {} chunks for user '{}'", file.getOriginalFilename(), chunks.size(), username);
        return document;
    }




}
