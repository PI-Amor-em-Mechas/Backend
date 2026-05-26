package com.evolutionbot.web;

import com.evolutionbot.service.ConversationService;
import com.evolutionbot.service.EvolutionApiClient;
import com.evolutionbot.web.dto.SendMessageRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final ConversationService conversationService;
    private final EvolutionApiClient evolutionApiClient;

    public AttendanceController(ConversationService conversationService,
                                EvolutionApiClient evolutionApiClient) {
        this.conversationService = conversationService;
        this.evolutionApiClient = evolutionApiClient;
    }

    @PostMapping("/handoff/{phoneNumber}")
    public ResponseEntity<Void> handoff(@PathVariable String phoneNumber) {
        conversationService.setHumanMode(phoneNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resume/{phoneNumber}")
    public ResponseEntity<Void> resume(@PathVariable String phoneNumber) {
        conversationService.setBotMode(phoneNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> send(@Valid @RequestBody SendMessageRequest request) {
        String externalId = evolutionApiClient.sendText(request.phoneNumber(), request.text());
        return ResponseEntity.ok(Map.of("externalMessageId", externalId == null ? "" : externalId));
    }
}
