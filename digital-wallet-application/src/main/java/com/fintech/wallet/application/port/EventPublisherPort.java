package com.fintech.wallet.application.port;

import com.fintech.wallet.common.model.DomainEvent;
import java.util.List;

public interface EventPublisherPort {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}
