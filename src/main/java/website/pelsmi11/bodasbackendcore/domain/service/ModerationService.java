package website.pelsmi11.bodasbackendcore.domain.service;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import website.pelsmi11.bodasbackendcore.domain.dto.ModerationWebhookRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.feed.PhotoFeedPublisher;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.util.Map;

/**
 * Applies AI moderation decisions to persisted photos.
 */
@Service
public class ModerationService {

    private final PhotoRepository photoRepository;
    private final PhotoFeedPublisher photoFeedPublisher;

    public ModerationService(
            PhotoRepository photoRepository,
            PhotoFeedPublisher photoFeedPublisher
    ) {
        this.photoRepository = photoRepository;
        this.photoFeedPublisher = photoFeedPublisher;
    }

    /**
     * Updates photo moderation status from the webhook verdict.
     */
    @Transactional
    public void processModerationResult(ModerationWebhookRequest request) {
        Photo photo = photoRepository.findFirstByS3Key(request.s3Key())
                .orElseThrow(() -> CustomErrorException.handlerCustomError("Photo not found", HttpStatus.NOT_FOUND));

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
            photoFeedPublisher.publishApprovedPhoto(photo);
        }
    }
}
