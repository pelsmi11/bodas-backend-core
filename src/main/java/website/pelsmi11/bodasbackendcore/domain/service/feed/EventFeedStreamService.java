package website.pelsmi11.bodasbackendcore.domain.service.feed;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active SSE clients and dispatches live feed messages per event token.
 */
@Service
public class EventFeedStreamService {

    private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emittersByEvent = new ConcurrentHashMap<>();

    /**
     * Registers and returns a new emitter for the requested event token.
     */
    public SseEmitter subscribe(String eventToken) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        List<SseEmitter> eventEmitters = emittersByEvent.computeIfAbsent(eventToken, key -> new CopyOnWriteArrayList<>());
        eventEmitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(eventToken, emitter));
        emitter.onTimeout(() -> removeEmitter(eventToken, emitter));
        emitter.onError(error -> removeEmitter(eventToken, emitter));

        return emitter;
    }

    /**
     * Pushes a "new-photo" event to all connected clients for an event.
     */
    public void dispatchNewPhoto(String eventToken, PhotoFeedDto payload) {
        List<SseEmitter> eventEmitters = emittersByEvent.get(eventToken);
        if (eventEmitters == null || eventEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : eventEmitters) {
            try {
                emitter.send(SseEmitter.event().name("new-photo").data(payload));
            } catch (IOException exception) {
                emitter.complete();
                removeEmitter(eventToken, emitter);
            }
        }
    }

    private void removeEmitter(String eventToken, SseEmitter emitter) {
        List<SseEmitter> eventEmitters = emittersByEvent.get(eventToken);
        if (eventEmitters == null) {
            return;
        }
        eventEmitters.remove(emitter);
        if (eventEmitters.isEmpty()) {
            emittersByEvent.remove(eventToken);
        }
    }
}
