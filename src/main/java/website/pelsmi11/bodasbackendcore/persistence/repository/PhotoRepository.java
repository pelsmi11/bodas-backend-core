package website.pelsmi11.bodasbackendcore.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    Optional<Photo> findFirstByS3Key(String s3Key);

    List<Photo> findByEventTokenAndStatusOrderByUploadedAtDesc(String eventToken, ModerationStatus status);

    Page<Photo> findByEventIdAndDeletedAtIsNullOrderByUploadedAtDesc(Integer eventId, Pageable pageable);

    Page<Photo> findByEventIdOrderByUploadedAtDesc(Integer eventId, Pageable pageable);

    Page<Photo> findByEventIdAndStatusAndDeletedAtIsNullOrderByUploadedAtDesc(Integer eventId, ModerationStatus status, Pageable pageable);

    Page<Photo> findByStatusAndDeletedAtIsNullOrderByUploadedAtDesc(ModerationStatus status, Pageable pageable);

    long countByEventId(Integer eventId);

    long countByEventIdAndStatus(Integer eventId, ModerationStatus status);

    long countByEventIdAndDeletedAtIsNotNull(Integer eventId);

    List<Photo> findByIdIn(List<UUID> ids);

    @Modifying
    @Query("UPDATE Photo p SET p.deletedAt = :deletedAt WHERE p.id IN :ids AND p.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") OffsetDateTime deletedAt);
}
