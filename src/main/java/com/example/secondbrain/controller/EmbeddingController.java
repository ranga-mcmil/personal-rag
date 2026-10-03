package com.example.secondbrain.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.secondbrain.service.ChunkEmbeddingService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor 
public class EmbeddingController {

    private final ChunkEmbeddingService chunkEmbeddingService;

    @PostMapping("/embed")
    public String embed(Authentication authentication) {
        int count = chunkEmbeddingService.embedPendingChunks(authentication.getName());
        return "Embedded " + count + " chunks";
    }

}
