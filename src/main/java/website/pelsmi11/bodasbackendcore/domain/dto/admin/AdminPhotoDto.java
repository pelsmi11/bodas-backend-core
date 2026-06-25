package website.pelsmi11.bodasbackendcore.domain.dto.admin;

import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AdminPhotoDto(
        UUID id,
        String s3Key,
        ModerationStatus status,
        String userName,
        String userCognitoId,
        UUID guestUuid,
        String eventToken,
        Integer eventId,
        OffsetDateTime uploadedAt,
        OffsetDateTime moderatedAt,
        String moderatedBy,
        Map<String, Object> moderationDetails,
        OffsetDateTime deletedAt
) {
}
