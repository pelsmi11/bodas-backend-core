package website.pelsmi11.bodasbackendcore.web.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import website.pelsmi11.bodasbackendcore.domain.dto.ApiResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.domain.service.feed.EventFeedService;
import website.pelsmi11.bodasbackendcore.domain.service.feed.EventFeedStreamService;

import java.util.List;

/**
 * Event photo feed endpoints: initial snapshot plus live stream updates.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventFeedController {

    private final EventFeedService eventFeedService;
    private final EventFeedStreamService eventFeedStreamService;

    public EventFeedController(EventFeedService eventFeedService, EventFeedStreamService eventFeedStreamService) {
        this.eventFeedService = eventFeedService;
        this.eventFeedStreamService = eventFeedStreamService;
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
        return eventFeedStreamService.subscribe(eventToken);
    }
}
