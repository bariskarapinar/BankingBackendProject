package com.fintech.wallet.application.port;

import com.fintech.wallet.application.event.OutboxEvent;
import java.util.List;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);
    List<OutboxEvent> findUnpublished(int limit);
    void markAsPublished(String eventId);
}
