package cn.edu.techgroup.outsourcing.modules.user.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class MemberAccessChangedPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public MemberAccessChangedPublisher(
            ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(String account) {
        eventPublisher.publishEvent(new MemberAccessChangedEvent(account));
    }
}
