package website.pelsmi11.bodasbackendcore.domain.dto;

/**
 * Internal Redis payload. The S3 key must never be returned by public endpoints.
 */
public record PhotoFeedEvent(
        String photoId,
        String s3Key,
        String uploadedAt
) {
}
