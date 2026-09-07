package website.pelsmi11.bodasbackendcore.web.controller;

import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import website.pelsmi11.bodasbackendcore.domain.dto.ApiResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.domain.dto.PublicEventDto;
import website.pelsmi11.bodasbackendcore.domain.service.feed.EventFeedService;
import website.pelsmi11.bodasbackendcore.domain.service.feed.EventFeedStreamService;
import website.pelsmi11.bodasbackendcore.domain.service.photo.EventResolver;

import java.util.List;

/**
 * Event photo feed endpoints: initial snapshot plus live stream updates.
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Feed", description = "Approved photo feed snapshot and live SSE stream")
public class EventFeedController {

    private final EventFeedService eventFeedService;
    private final EventFeedStreamService eventFeedStreamService;
    private final EventResolver eventResolver;

    public EventFeedController(
            EventFeedService eventFeedService,
            EventFeedStreamService eventFeedStreamService,
            EventResolver eventResolver
    ) {
        this.eventFeedService = eventFeedService;
        this.eventFeedStreamService = eventFeedStreamService;
        this.eventResolver = eventResolver;
    }

    @GetMapping("/{eventToken}")
    public ApiResponse<PublicEventDto> getEventDetails(@PathVariable String eventToken) {
        return ApiResponse.ok(eventFeedService.getEventDetails(eventToken));
    }

    /**
     * Returns currently approved photos for an event.
     */
    @GetMapping("/{eventToken}/feed")
    public ApiResponse<List<PhotoFeedDto>> getInitialFeed(@PathVariable String eventToken) {
        return ApiResponse.ok(eventFeedService.getApprovedFeed(eventToken));
    }

    /**
     * Opens a long-lived SSE stream for new approved photos.
     */
    @GetMapping(value = "/{eventToken}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLivePhotos(@PathVariable String eventToken) {
        eventResolver.findActiveEvent(eventToken);
        return eventFeedStreamService.subscribe(eventToken);
    }
}
