package website.pelsmi11.bodasbackendcore.domain.service.feed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;

/**
 * Publishes approved photo events to the Redis pub/sub channel consumed by
 * the live SSE feed. Shared by webhook-driven and admin-driven moderation.
 */
@Component
public class PhotoFeedPublisher {

    private static final String FEED_CHANNEL_PREFIX = "event-feed:";

    private final EventFeedService eventFeedService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PhotoFeedPublisher(
            EventFeedService eventFeedService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.eventFeedService = eventFeedService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes an APPROVED photo to the event feed channel.
     */
    public void publishApprovedPhoto(Photo photo) {
        try {
            PhotoFeedDto payload = eventFeedService.toDto(photo);
            String jsonMessage = objectMapper.writeValueAsString(payload);
            String channel = FEED_CHANNEL_PREFIX + photo.getEvent().getToken();
            redisTemplate.convertAndSend(channel, jsonMessage);
        } catch (JsonProcessingException exception) {
            throw CustomErrorException.handlerCustomError("Failed to serialize feed event", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
