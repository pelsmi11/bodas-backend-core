package website.pelsmi11.bodasbackendcore.domain.service.feed;

import org.springframework.stereotype.Service;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Provides approved photo feed data for event screens.
 */
@Service
public class EventFeedService {

    private final PhotoRepository photoRepository;

    public EventFeedService(PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    /**
     * Returns approved photos for an event ordered by upload time descending.
     */
    public List<PhotoFeedDto> getApprovedFeed(String eventToken) {
        return photoRepository.findByEventTokenAndStatusOrderByUploadedAtDesc(eventToken, ModerationStatus.APPROVED)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Maps a Photo entity into transport payload used by feed and stream.
     */
    public PhotoFeedDto toDto(Photo photo) {
        String userName = photo.getUser() != null ? photo.getUser().getName() : null;
        OffsetDateTime uploadedAt = photo.getUploadedAt();
        return new PhotoFeedDto(
                photo.getId().toString(),
                photo.getS3Key(),
                userName,
                uploadedAt != null ? uploadedAt.toString() : null
        );
    }
}
