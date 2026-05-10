package website.pelsmi11.bodasbackendcore.domain.service;

import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.domain.service.feed.EventFeedService;
import website.pelsmi11.bodasbackendcore.domain.dto.ModerationWebhookRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Applies AI moderation decisions to persisted photos.
 */
@Service
public class ModerationService {

    private static final String FEED_CHANNEL_PREFIX = "event-feed:";

    private final PhotoRepository photoRepository;
    private final EventFeedService eventFeedService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ModerationService(
            PhotoRepository photoRepository,
            EventFeedService eventFeedService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.photoRepository = photoRepository;
        this.eventFeedService = eventFeedService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Updates photo moderation status from the webhook verdict.
     */
    @Transactional
    public void processModerationResult(ModerationWebhookRequest request) {
        Photo photo = photoRepository.findFirstByS3Key(request.s3Key())
                .orElseThrow(() -> CustomErrorException.handlerCustomError("Foto no encontrada", HttpStatus.NOT_FOUND));

        ModerationStatus status = request.isApproved() ? ModerationStatus.APPROVED : ModerationStatus.REJECTED;
        photo.setStatus(status);

        if (request.reason() != null && !request.reason().isBlank()) {
            photo.setModerationDetails(Map.of(
                    "reason", request.reason(),
                    "source", "lambda-webhook"
            ));
        }

        photoRepository.save(photo);

        if (status == ModerationStatus.APPROVED) {
            publishApprovedPhoto(photo);
        }
    }

    private void publishApprovedPhoto(Photo photo) {
        try {
            PhotoFeedDto payload = eventFeedService.toDto(photo);
            String jsonMessage = objectMapper.writeValueAsString(payload);
            String channel = FEED_CHANNEL_PREFIX + photo.getEvent().getToken();
            redisTemplate.convertAndSend(channel, jsonMessage);
        } catch (JsonProcessingException exception) {
            throw CustomErrorException.handlerCustomError("No se pudo serializar el evento de feed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
