package com.evolutionbot.service;

import com.evolutionbot.domain.conversation.Conversation;
import com.evolutionbot.domain.conversation.ConversationStatus;
import com.evolutionbot.domain.message.Message;
import com.evolutionbot.repository.ConversationRepository;
import com.evolutionbot.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Conversation findOrCreateByPhone(String phoneNumber) {
        return conversationRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setPhoneNumber(phoneNumber);
                    conversation.setStatus(ConversationStatus.BOT_ACTIVE);
                    return conversationRepository.save(conversation);
                });
    }

    @Transactional
    public void saveInboundMessage(Conversation conversation, String externalMessageId, String content) {
        conversation.setLastMessageAt(OffsetDateTime.now());
        conversationRepository.save(conversation);

        Message message = new Message();
        message.setConversation(conversation);
        message.setExternalMessageId(externalMessageId);
        message.setDirection("INBOUND");
        message.setContent(content);
        messageRepository.save(message);
    }

    @Transactional
    public void saveOutboundMessage(Conversation conversation, String externalMessageId, String content) {
        conversation.setLastMessageAt(OffsetDateTime.now());
        conversationRepository.save(conversation);

        Message message = new Message();
        message.setConversation(conversation);
        message.setExternalMessageId(externalMessageId);
        message.setDirection("OUTBOUND");
        message.setContent(content);
        messageRepository.save(message);
    }

    @Transactional
    public void setHumanMode(String phoneNumber) {
        Conversation conversation = findOrCreateByPhone(phoneNumber);
        conversation.setStatus(ConversationStatus.HUMAN_ACTIVE);
        conversationRepository.save(conversation);
    }

    @Transactional
    public void setBotMode(String phoneNumber) {
        Conversation conversation = findOrCreateByPhone(phoneNumber);
        conversation.setStatus(ConversationStatus.BOT_ACTIVE);
        conversationRepository.save(conversation);
    }
}
