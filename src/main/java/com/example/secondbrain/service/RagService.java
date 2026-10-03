package com.example.secondbrain.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.secondbrain.dto.ChatAnswer;
import com.example.secondbrain.dto.RetrievedChunk;
import com.pgvector.PGvector;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final ChatClient chatClient;

    private static final int TOP_K = 5;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are a helpful assistant answering questions using ONLY the context provided below, \
            which comes from the user's own uploaded documents.
            If the answer isn't contained in the context, say you don't know rather than guessing \
            or using outside knowledge.
            Mention which source file(s) your answer came from.

            Context:
            %s
            """;

    public ChatAnswer ask(String question, String username) {
        List<RetrievedChunk> retrieved = retrieveRelevantChunks(question, username, TOP_K);

        if (retrieved.isEmpty()) {
            return new ChatAnswer("You don't have any embedded documents yet — upload and embed something first.", List.of());
        }

        String context = retrieved.stream()
                .map(r -> "[Source: " + r.filename() + "]\n" + r.content())
                .collect(Collectors.joining("\n\n---\n\n"));

        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT_TEMPLATE.formatted(context))
                .user(question)
                .call()
                .content();

        List<String> sources = retrieved.stream().map(RetrievedChunk::filename).distinct().toList();

        return new ChatAnswer(answer, sources);
    }

    private List<RetrievedChunk> retrieveRelevantChunks(String question, String username, int limit) {
        float[] queryVector = embeddingModel.embed("search_query: " + question);
        PGvector pgQueryVector = new PGvector(queryVector);

        return jdbcTemplate.query(
                "SELECT c.content, d.filename, 1 - (c.embedding <=> ?) AS similarity " +
                        "FROM document_chunk c " +
                        "JOIN document d ON c.document_id = d.id " +
                        "JOIN app_user u ON d.owner_id = u.id " +
                        "WHERE u.username = ? AND c.embedding IS NOT NULL " +
                        "ORDER BY c.embedding <=> ? " +
                        "LIMIT ?",
                (rs, rowNum) -> new RetrievedChunk(rs.getString("content"), rs.getString("filename"), rs.getDouble("similarity")),
                pgQueryVector, username, pgQueryVector, limit
        );
    }

}
