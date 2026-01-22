package com.podcast.indexer.controller;

import com.podcast.indexer.dto.AnswerResponse;
import com.podcast.indexer.dto.AskQuestionRequest;
import com.podcast.indexer.service.QuestionAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QuestionAnswerController {
    
    private final QuestionAnswerService questionAnswerService;
    
    @PostMapping("/ask")
    public ResponseEntity<AnswerResponse> askQuestion(@RequestBody AskQuestionRequest request) {
        try {
            AnswerResponse answer = questionAnswerService.answerQuestion(
                    request.getPodcastId(), 
                    request.getQuestion());
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
