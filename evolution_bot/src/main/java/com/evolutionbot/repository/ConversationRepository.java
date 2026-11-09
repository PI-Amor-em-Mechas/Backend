package com.evolutionbot.repository;

import com.evolutionbot.domain.conversation.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByPhoneNumber(String phoneNumber);
}
