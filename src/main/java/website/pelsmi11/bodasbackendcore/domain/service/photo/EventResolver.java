package website.pelsmi11.bodasbackendcore.domain.service.photo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.repository.EventRepository;

/**
 * Resolves an active event from a public event token.
 */
@Component
public class EventResolver {

    private final EventRepository eventRepository;

    public EventResolver(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Returns an active event or throws 404 when the token is invalid/inactive.
     */
    public Event findActiveEvent(String eventToken) {
        return eventRepository.findFirstByToken(eventToken)
                .filter(event -> Boolean.TRUE.equals(event.getIsActive()))
                .orElseThrow(() -> CustomErrorException.handlerCustomError("Event not found or inactive", HttpStatus.NOT_FOUND));
    }
}
