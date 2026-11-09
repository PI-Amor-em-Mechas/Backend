package com.evolutionbot.repository;

import com.evolutionbot.domain.event.InboundEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundEventRepository extends JpaRepository<InboundEvent, Long> {
    boolean existsByEventId(String eventId);
}
