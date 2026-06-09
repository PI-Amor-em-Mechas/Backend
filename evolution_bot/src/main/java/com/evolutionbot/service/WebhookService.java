package com.evolutionbot.service;

import com.evolutionbot.domain.conversation.Conversation;
import com.evolutionbot.domain.conversation.ConversationStatus;
import com.evolutionbot.domain.event.InboundEvent;
import com.evolutionbot.repository.InboundEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private final InboundEventRepository inboundEventRepository;
    private final ConversationService conversationService;
    private final EvolutionApiClient evolutionApiClient;

    public WebhookService(InboundEventRepository inboundEventRepository,
                          ConversationService conversationService,
                          EvolutionApiClient evolutionApiClient) {
        this.inboundEventRepository = inboundEventRepository;
        this.conversationService = conversationService;
        this.evolutionApiClient = evolutionApiClient;
    }

    @Transactional
    public void process(JsonNode payload) {
        String eventId = payload.path("event").asText("") + ":" + payload.path("data").path("key").path("id").asText("");
        if (eventId.isBlank() || inboundEventRepository.existsByEventId(eventId)) {
            return;
        }

        InboundEvent inboundEvent = new InboundEvent();
        inboundEvent.setEventId(eventId);
        inboundEventRepository.save(inboundEvent);

        String phone = payload.path("data").path("key").path("remoteJid").asText("")
                .replace("@s.whatsapp.net", "");

        if (phone.isBlank()) {
            return;
        }

        Conversation conversation = conversationService.findOrCreateByPhone(phone);
        String externalMessageId = payload.path("data").path("key").path("id").asText(null);
        String incomingText = evolutionApiClient.extractText(payload);

        if (incomingText == null || incomingText.isBlank()) {
            return;
        }

        conversationService.saveInboundMessage(conversation, externalMessageId, incomingText);

        if (conversation.getStatus() == ConversationStatus.HUMAN_ACTIVE) {
            return;
        }

        String responseText = buildResponse(incomingText);
        String outboundId = evolutionApiClient.sendText(phone, responseText);
        conversationService.saveOutboundMessage(conversation, outboundId, responseText);
    }

    private String buildResponse(String incomingText) {
        String text = incomingText.toLowerCase();

        if (text.contains("financeiro")) {
            return "Perfeito. Vou te direcionar para o time financeiro. Posso confirmar seu nome completo?";
        }

        if (text.contains("suporte")) {
            return "Entendi. Vou te ajudar com suporte. Pode me dizer em uma frase o problema principal?";
        }

        if (text.contains("comercial") || text.contains("orcamento") || text.contains("orçamento")) {
            return "Ótimo. Para comercial, me passe o serviço desejado e cidade para agilizarmos o orçamento.";
        }

        if (text.contains("atendente") || text.contains("humano")) {
            return "Sem problemas. Vou transferir seu atendimento para uma pessoa do time agora.";
        }

        return "Oi. Sou o assistente virtual. Digite: financeiro, suporte, comercial ou falar com atendente.";
    }
}
