package website.pelsmi11.bodasbackendcore.domain.dto;

import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;

/**
 * Result returned after an uploaded S3 object is registered for moderation.
 */
public record PhotoConfirmationResponse(
        String photoId,
        ModerationStatus status
) {
}
