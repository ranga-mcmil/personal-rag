package com.example.secondbrain.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import com.example.secondbrain.entity.Document;
import com.example.secondbrain.service.DocumentIngestionService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor 
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;

    @PostMapping("/documents")
    public Document upload(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        return documentIngestionService.ingest(file, authentication.getName());
    }
}
