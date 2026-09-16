package com.fintech.wallet.infrastructure.adapter;

import com.fintech.wallet.application.event.OutboxEvent;
import com.fintech.wallet.application.port.OutboxEventRepository;
import com.fintech.wallet.common.exception.InfrastructureException;
import com.fintech.wallet.infrastructure.persistence.OutboxEventJpaEntity;
import com.fintech.wallet.infrastructure.persistence.OutboxEventSpringDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {
    private final OutboxEventSpringDataRepository outboxEventSpringDataRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        try {
            OutboxEventJpaEntity entity = mapToJpaEntity(event);
            OutboxEventJpaEntity savedEntity = outboxEventSpringDataRepository.save(entity);
            log.debug("Outbox event saved: {}", event.getId());
            return mapToDomainEvent(savedEntity);
        } catch (Exception e) {
            log.error("Failed to save outbox event", e);
            throw new InfrastructureException(
                    "Failed to save outbox event",
                    "OUTBOX_SAVE_FAILED",
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public List<OutboxEvent> findUnpublished(int limit) {
        try {
            List<OutboxEventJpaEntity> entities = outboxEventSpringDataRepository.findUnpublishedEvents(limit);
            return entities.stream()
                    .map(this::mapToDomainEvent)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find unpublished outbox events", e);
            throw new InfrastructureException(
                    "Failed to find unpublished outbox events",
                    "OUTBOX_FIND_FAILED",
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void markAsPublished(String eventId) {
        try {
            outboxEventSpringDataRepository.findById(eventId).ifPresent(entity -> {
                entity.setPublished(true);
                entity.setPublishedAt(Instant.now());
                outboxEventSpringDataRepository.save(entity);
                log.debug("Outbox event marked as published: {}", eventId);
            });
        } catch (Exception e) {
            log.error("Failed to mark outbox event as published: {}", eventId, e);
            throw new InfrastructureException(
                    "Failed to mark outbox event as published",
                    "OUTBOX_PUBLISH_MARK_FAILED",
                    e.getMessage(),
                    e
            );
        }
    }

    private OutboxEventJpaEntity mapToJpaEntity(OutboxEvent event) {
        return OutboxEventJpaEntity.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .aggregateId(event.getAggregateId())
                .payload(event.getPayload())
                .published(event.isPublished())
                .publishedAt(event.getPublishedAt())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private OutboxEvent mapToDomainEvent(OutboxEventJpaEntity entity) {
        OutboxEvent event = new OutboxEvent(null, entity.getPayload());

        java.lang.reflect.Field[] fields = OutboxEvent.class.getDeclaredFields();
        try {
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                if (field.getName().equals("id")) {
                    field.set(event, entity.getId());
                } else if (field.getName().equals("eventType")) {
                    field.set(event, entity.getEventType());
                } else if (field.getName().equals("aggregateId")) {
                    field.set(event, entity.getAggregateId());
                } else if (field.getName().equals("published")) {
                    field.set(event, entity.isPublished());
                } else if (field.getName().equals("publishedAt")) {
                    field.set(event, entity.getPublishedAt());
                } else if (field.getName().equals("createdAt")) {
                    field.set(event, entity.getCreatedAt());
                }
            }
        } catch (IllegalAccessException e) {
            throw new InfrastructureException(
                    "Failed to map outbox event via reflection",
                    "REFLECTION_ERROR",
                    e.getMessage(),
                    e
            );
        }

        return event;
    }
}
