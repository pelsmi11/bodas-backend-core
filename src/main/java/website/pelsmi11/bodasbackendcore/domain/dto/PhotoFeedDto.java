package website.pelsmi11.bodasbackendcore.domain.dto;

/**
 * Lightweight payload used by event feed endpoints and live stream updates.
 */
public record PhotoFeedDto(
        String photoId,
        String s3Key,
        String userName,
        String uploadedAt
) {
}
