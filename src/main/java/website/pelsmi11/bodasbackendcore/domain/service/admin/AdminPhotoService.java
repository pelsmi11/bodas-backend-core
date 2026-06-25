package website.pelsmi11.bodasbackendcore.domain.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminPhotoDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.PhotoModerationRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.PhotoStatusUpdateRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.feed.PhotoFeedPublisher;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Administrative operations for photo moderation and lifecycle.
 */
@Service
public class AdminPhotoService {

    private final PhotoRepository photoRepository;
    private final PhotoFeedPublisher photoFeedPublisher;

    public AdminPhotoService(
            PhotoRepository photoRepository,
            PhotoFeedPublisher photoFeedPublisher
    ) {
        this.photoRepository = photoRepository;
        this.photoFeedPublisher = photoFeedPublisher;
    }

    @Transactional(readOnly = true)
    public Page<AdminPhotoDto> findByEvent(Integer eventId, ModerationStatus status, boolean includeDeleted, Pageable pageable) {
        Page<Photo> page;
        if (status != null) {
            page = photoRepository.findByEventIdAndStatusAndDeletedAtIsNullOrderByUploadedAtDesc(eventId, status, pageable);
        } else if (includeDeleted) {
            page = photoRepository.findByEventIdOrderByUploadedAtDesc(eventId, pageable);
        } else {
            page = photoRepository.findByEventIdAndDeletedAtIsNullOrderByUploadedAtDesc(eventId, pageable);
        }
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AdminPhotoDto> findPendingGlobal(Pageable pageable) {
        return photoRepository.findByStatusAndDeletedAtIsNullOrderByUploadedAtDesc(ModerationStatus.PENDING, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AdminPhotoDto findById(UUID id) {
        return toDto(requirePhoto(id));
    }

    @Transactional
    public AdminPhotoDto updateStatus(UUID photoId, PhotoStatusUpdateRequest request, String adminSub) {
        Photo photo = requirePhoto(photoId);
        ModerationStatus previous = photo.getStatus();
        ModerationStatus next = request.getStatus();
        photo.setStatus(next);
        photo.setModeratedAt(OffsetDateTime.now());
        photo.setModeratedBy(adminSub);
        photo.setModerationDetails(buildDetails(request.getReason(), "admin-manual", previous));
        Photo saved = photoRepository.save(photo);
        if (next == ModerationStatus.APPROVED) {
            photoFeedPublisher.publishApprovedPhoto(saved);
        }
        return toDto(saved);
    }

    @Transactional
    public List<AdminPhotoDto> moderate(PhotoModerationRequest request, String adminSub) {
        List<Photo> photos = photoRepository.findByIdIn(request.getPhotoIds());
        if (photos.isEmpty()) {
            throw CustomErrorException.handlerCustomError("Requested photos not found", HttpStatus.NOT_FOUND);
        }
        ModerationStatus target = request.getAction() == PhotoModerationRequest.ModerationAction.APPROVE
                ? ModerationStatus.APPROVED
                : ModerationStatus.REJECTED;
        OffsetDateTime now = OffsetDateTime.now();
        for (Photo photo : photos) {
            ModerationStatus previous = photo.getStatus();
            photo.setStatus(target);
            photo.setModeratedAt(now);
            photo.setModeratedBy(adminSub);
            photo.setModerationDetails(buildDetails(request.getReason(), "admin-manual", previous));
        }
        List<Photo> saved = photoRepository.saveAll(photos);
        if (target == ModerationStatus.APPROVED) {
            saved.forEach(photoFeedPublisher::publishApprovedPhoto);
        }
        return saved.stream().map(this::toDto).toList();
    }

    @Transactional
    public int softDelete(String ids) {
        if (ids == null || ids.isBlank()) {
            throw CustomErrorException.handlerCustomError("Parameter 'ids' is required", HttpStatus.BAD_REQUEST);
        }
        List<UUID> photoIds = parseIds(ids);
        if (photoIds.isEmpty()) {
            return 0;
        }
        return photoRepository.softDeleteByIds(photoIds, OffsetDateTime.now());
    }

    private List<UUID> parseIds(String ids) {
        try {
            return Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(UUID::fromString)
                    .toList();
        } catch (IllegalArgumentException ex) {
            throw CustomErrorException.handlerCustomError("Invalid UUID format in 'ids'", HttpStatus.BAD_REQUEST);
        }
    }

    private Photo requirePhoto(UUID id) {
        return photoRepository.findById(id)
                .orElseThrow(() -> CustomErrorException.handlerCustomError("Photo not found", HttpStatus.NOT_FOUND));
    }

    private Map<String, Object> buildDetails(String reason, String source, ModerationStatus previous) {
        Map<String, Object> details = new HashMap<>();
        details.put("source", source);
        if (reason != null && !reason.isBlank()) {
            details.put("reason", reason);
        }
        if (previous != null) {
            details.put("previousStatus", previous.name());
        }
        return details;
    }

    private AdminPhotoDto toDto(Photo photo) {
        return new AdminPhotoDto(
                photo.getId(),
                photo.getS3Key(),
                photo.getStatus(),
                photo.getUser() != null ? photo.getUser().getName() : null,
                photo.getUser() != null ? photo.getUser().getCognitoId() : null,
                photo.getDevice() != null ? photo.getDevice().getGuestUuid() : null,
                photo.getEvent() != null ? photo.getEvent().getToken() : null,
                photo.getEvent() != null ? photo.getEvent().getId() : null,
                photo.getUploadedAt(),
                photo.getModeratedAt(),
                photo.getModeratedBy(),
                photo.getModerationDetails(),
                photo.getDeletedAt()
        );
    }
}
