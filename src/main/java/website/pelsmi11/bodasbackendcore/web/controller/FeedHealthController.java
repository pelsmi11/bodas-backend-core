package website.pelsmi11.bodasbackendcore.web.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import website.pelsmi11.bodasbackendcore.domain.dto.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health endpoint focused on the live feed stack (Redis + Pub/Sub listener).
 */
@RestController
@RequestMapping("/api/v1/health")
public class FeedHealthController {

    private final RedisConnectionFactory redisConnectionFactory;
    private final ObjectProvider<RedisMessageListenerContainer> listenerContainerProvider;

    @Value("${app.redis.listener.enabled:true}")
    private boolean listenerEnabled;

    public FeedHealthController(
            RedisConnectionFactory redisConnectionFactory,
            ObjectProvider<RedisMessageListenerContainer> listenerContainerProvider
    ) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.listenerContainerProvider = listenerContainerProvider;
    }

    /**
     * Returns 200 when Redis is reachable and listener state is consistent.
     * Returns 503 when Redis ping fails.
     */
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> feedHealth() {
        Map<String, Object> status = new LinkedHashMap<>();

        boolean redisUp = pingRedis();
        status.put("redis", redisUp ? "UP" : "DOWN");

        RedisMessageListenerContainer container = listenerContainerProvider.getIfAvailable();
        status.put("listenerEnabled", listenerEnabled);
        status.put("listenerBeanPresent", container != null);
        status.put("listenerRunning", container != null && container.isRunning());

        HttpStatus responseStatus = redisUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        status.put("overall", redisUp ? "UP" : "DOWN");

        return ResponseEntity.status(responseStatus).body(ApiResponse.ok(status));
    }

    private boolean pingRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String ping = connection.ping();
            return "PONG".equalsIgnoreCase(ping);
        } catch (Exception exception) {
            return false;
        }
    }
}
