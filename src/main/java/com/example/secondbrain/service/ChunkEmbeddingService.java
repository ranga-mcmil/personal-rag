package com.example.secondbrain.service;

import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.secondbrain.entity.DocumentChunk;
import com.example.secondbrain.repository.DocumentChunkRepository;
import com.pgvector.PGvector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@RequiredArgsConstructor 
@Slf4j 
public class ChunkEmbeddingService {

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 50;

    public int embedPendingChunks(String username) {
        List<DocumentChunk> pending = documentChunkRepository.findPendingByOwner(username);
        int embedded = 0;

        for (int i = 0; i < pending.size(); i += BATCH_SIZE) {
            List<DocumentChunk> batch = pending.subList(i, Math.min(i + BATCH_SIZE, pending.size()));

            List<String> texts = batch.stream()
                    .map(c -> "search_document: " + c.getContent())
                    .toList();

            List<float[]> vectors = embeddingModel.embed(texts);

            for (int j = 0; j < batch.size(); j++) {
                DocumentChunk chunk = batch.get(j);
                jdbcTemplate.update(
                        "UPDATE document_chunk SET embedding = ?, embedded = true WHERE id = ?",
                        new PGvector(vectors.get(j)),
                        chunk.getId()
                );
            }

            embedded += batch.size();
            log.info("Embedded {} of {} pending chunks for user '{}'", embedded, pending.size(), username);
        }

        return embedded;
    }

}
