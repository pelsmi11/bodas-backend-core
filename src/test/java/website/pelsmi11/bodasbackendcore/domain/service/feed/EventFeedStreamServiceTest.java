package website.pelsmi11.bodasbackendcore.domain.service.feed;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventFeedStreamServiceTest {

    private final EventFeedStreamService service = new EventFeedStreamService();

    @Test
    void subscribe_returnsEmitterAndRegistersIt() {
        SseEmitter emitter = service.subscribe("ev123456");

        assertThat(emitter).isNotNull();
        assertThat(getEmittersMap().get("ev123456")).hasSize(1);
    }

    @Test
    void subscribe_multipleClients_sameEventList() {
        service.subscribe("ev123456");
        service.subscribe("ev123456");

        assertThat(getEmittersMap().get("ev123456")).hasSize(2);
    }

    @Test
    void dispatchNewPhoto_noEmitters_doesNothing() {
        PhotoFeedDto payload = new PhotoFeedDto("id", "key", "user", "2026-01-01T00:00:00Z");

        service.dispatchNewPhoto("nonexistent", payload);

        assertThat(getEmittersMap()).isEmpty();
    }

    @Test
    void dispatchNewPhoto_withEmitters_keepsEmittersAfterSuccessfulSend() {
        service.subscribe("ev123456");
        service.subscribe("ev123456");
        PhotoFeedDto payload = new PhotoFeedDto("id", "key", "user", "2026-01-01T00:00:00Z");

        service.dispatchNewPhoto("ev123456", payload);

        assertThat(getEmittersMap().get("ev123456")).hasSize(2);
    }

    @Test
    void dispatchNewPhoto_failingEmitter_removesFromList() throws Exception {
        service.subscribe("ev123456");
        replaceWithFailingEmitter("ev123456");
        PhotoFeedDto payload = new PhotoFeedDto("id", "key", "user", "2026-01-01T00:00:00Z");

        service.dispatchNewPhoto("ev123456", payload);

        assertThat(getEmittersMap()).doesNotContainKey("ev123456");
    }

    @Test
    void dispatchNewPhoto_allEmittersFail_removesEventKey() throws Exception {
        service.subscribe("ev123456");
        replaceWithFailingEmitter("ev123456");
        PhotoFeedDto payload = new PhotoFeedDto("id", "key", "user", "2026-01-01T00:00:00Z");

        service.dispatchNewPhoto("ev123456", payload);

        assertThat(getEmittersMap()).doesNotContainKey("ev123456");
    }

    @Test
    void dispatchNewPhoto_emptyEmitterList_doesNothing() {
        getEmittersMap().put("ev123456", new java.util.concurrent.CopyOnWriteArrayList<>());
        PhotoFeedDto payload = new PhotoFeedDto("id", "key", "user", "2026-01-01T00:00:00Z");

        service.dispatchNewPhoto("ev123456", payload);

        assertThat(getEmittersMap().get("ev123456")).isEmpty();
    }

    private void replaceWithFailingEmitter(String eventToken) throws Exception {
        List<SseEmitter> emitters = getEmittersMap().get(eventToken);
        emitters.clear();
        emitters.add(new FailingSseEmitter());
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<SseEmitter>> getEmittersMap() {
        try {
            Field field = EventFeedStreamService.class.getDeclaredField("emittersByEvent");
            field.setAccessible(true);
            return (Map<String, List<SseEmitter>>) field.get(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class FailingSseEmitter extends SseEmitter {
        @Override
        public void send(SseEventBuilder builder) throws IOException {
            throw new IOException("broken pipe");
        }
    }
}
