package website.pelsmi11.bodasbackendcore.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.domain.service.feed.EventFeedStreamService;

import java.nio.charset.StandardCharsets;

/**
 * Subscribes to Redis feed channels and dispatches messages to active SSE clients.
 */
@Configuration
public class RedisConfig {

    @Bean
    @ConditionalOnProperty(name = "app.redis.listener.enabled", havingValue = "true", matchIfMissing = true)
    MessageListener feedMessageListener(
            EventFeedStreamService eventFeedStreamService,
            ObjectMapper objectMapper
    ) {
        return (message, pattern) -> {
            try {
                String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                String eventToken = extractEventToken(channel);

                PhotoFeedDto payload = objectMapper.readValue(body, PhotoFeedDto.class);
                eventFeedStreamService.dispatchNewPhoto(eventToken, payload);
            } catch (Exception ignored) {
                // Intentionally ignore malformed messages to keep listener resilient.
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.redis.listener.enabled", havingValue = "true", matchIfMissing = true)
    RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListener feedMessageListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(feedMessageListener, new PatternTopic("event-feed:*"));
        return container;
    }

    String extractEventToken(String channel) {
        String[] segments = channel.split(":", 2);
        return segments.length == 2 ? segments[1] : "";
    }
}
