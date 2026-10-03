package com.example.secondbrain.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.secondbrain.dto.AskRequest;
import com.example.secondbrain.dto.ChatAnswer;
import com.example.secondbrain.service.RagService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor 
public class AskController {

    private final RagService ragService;

    @PostMapping("/ask")
    public ChatAnswer ask(@RequestBody AskRequest request, Authentication authentication) {
        return ragService.ask(request.question(), authentication.getName());
    }

    

}
