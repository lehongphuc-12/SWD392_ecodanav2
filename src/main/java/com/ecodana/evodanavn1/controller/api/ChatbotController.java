package com.ecodana.evodanavn1.controller.api;

import com.ecodana.evodanavn1.service.AIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final AIService aiService;

    public ChatbotController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askChatbot(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Message cannot be empty."));
        }

        String reply = aiService.askAI(message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("reply", reply);

        return ResponseEntity.ok(response);
    }
}
