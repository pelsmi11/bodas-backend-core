package website.pelsmi11.bodasbackendcore.domain.service.feed;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.domain.dto.PublicEventDto;
import website.pelsmi11.bodasbackendcore.domain.service.photo.EventResolver;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.util.List;

/**
 * Provides approved photo feed data for event screens.
 */
@Service
public class EventFeedService {

    private final PhotoRepository photoRepository;
    private final EventResolver eventResolver;
    private final PhotoFeedMapper photoFeedMapper;
    private final int feedLimit;

    public EventFeedService(
            PhotoRepository photoRepository,
            EventResolver eventResolver,
            PhotoFeedMapper photoFeedMapper,
            @Value("${app.photos.feed-limit:120}") int feedLimit
    ) {
        this.photoRepository = photoRepository;
        this.eventResolver = eventResolver;
        this.photoFeedMapper = photoFeedMapper;
        this.feedLimit = feedLimit;
    }

    public PublicEventDto getEventDetails(String eventToken) {
        var event = eventResolver.findActiveEvent(eventToken);
        return new PublicEventDto(
                event.getToken(),
                event.getName(),
                event.getEventDate(),
                event.getDescription()
        );
    }

    /**
     * Returns approved photos for an event ordered by upload time descending.
     */
    public List<PhotoFeedDto> getApprovedFeed(String eventToken) {
        eventResolver.findActiveEvent(eventToken);
        return photoRepository.findByEventTokenAndStatusAndDeletedAtIsNullOrderByUploadedAtDesc(
                        eventToken,
                        ModerationStatus.APPROVED,
                        PageRequest.of(0, feedLimit)
                )
                .stream()
                .map(photoFeedMapper::toPublicDto)
                .toList();
    }
}
