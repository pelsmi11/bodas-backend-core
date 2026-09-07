package website.pelsmi11.bodasbackendcore.domain.service.feed;

import org.springframework.stereotype.Component;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedEvent;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;

import java.time.OffsetDateTime;

/**
 * Maps persisted and internal feed records to their public representation.
 */
@Component
public class PhotoFeedMapper {

    private final PhotoReadUrlService photoReadUrlService;

    public PhotoFeedMapper(PhotoReadUrlService photoReadUrlService) {
        this.photoReadUrlService = photoReadUrlService;
    }

    public PhotoFeedEvent toInternalEvent(Photo photo) {
        return new PhotoFeedEvent(
                photo.getId().toString(),
                photo.getS3Key(),
                formatTimestamp(photo.getUploadedAt())
        );
    }

    public PhotoFeedDto toPublicDto(Photo photo) {
        return toPublicDto(toInternalEvent(photo));
    }

    public PhotoFeedDto toPublicDto(PhotoFeedEvent event) {
        return new PhotoFeedDto(
                event.photoId(),
                photoReadUrlService.createReadUrl(event.s3Key()),
                event.uploadedAt()
        );
    }

    private String formatTimestamp(OffsetDateTime uploadedAt) {
        return uploadedAt != null ? uploadedAt.toString() : null;
    }
}
