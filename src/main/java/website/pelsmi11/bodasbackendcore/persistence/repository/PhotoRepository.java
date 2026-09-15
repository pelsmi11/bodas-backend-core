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

    List<Photo> findByEventTokenAndStatusAndDeletedAtIsNullOrderByUploadedAtDesc(
            String eventToken,
            ModerationStatus status,
            Pageable pageable
    );

    @Modifying
    @Query(value = """
            INSERT INTO photos (id, event_id, user_id, device_uuid, s3_key, status)
            VALUES (:id, :eventId, :userId, :deviceUuid, :s3Key, 'PENDING'::moderation_status)
            ON CONFLICT (s3_key) DO NOTHING
            """, nativeQuery = true)
    int insertPendingIfAbsent(
            @Param("id") UUID id,
            @Param("eventId") Integer eventId,
            @Param("userId") Integer userId,
            @Param("deviceUuid") UUID deviceUuid,
            @Param("s3Key") String s3Key
    );

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

    // ---- Ownership-scoped queries (admin caller = event admin_id) -----------

    Page<Photo> findByEvent_AdminIdAndDeletedAtIsNullOrderByUploadedAtDesc(String adminId, Pageable pageable);

    Page<Photo> findByEvent_AdminIdAndStatusAndDeletedAtIsNullOrderByUploadedAtDesc(
            String adminId, ModerationStatus status, Pageable pageable);

    boolean existsByIdAndEvent_AdminId(UUID id, String adminId);

    /** Events where the device uploaded photos (participation reconciliation). */
    @org.springframework.data.jpa.repository.Query(
            "SELECT DISTINCT p.event.id FROM Photo p WHERE p.device.guestUuid = :guestUuid AND p.deletedAt IS NULL")
    List<Integer> findDistinctEventIdsByDeviceUuid(@Param("guestUuid") UUID guestUuid);

    /** T2 transfer: re-point anonymous photos to the account user. */
    @Modifying
    @Query("UPDATE Photo p SET p.user.id = :toUserId WHERE p.user.id = :fromUserId")
    int transferPhotosBetweenUsers(@Param("fromUserId") Integer fromUserId, @Param("toUserId") Integer toUserId);


    List<Photo> findByEvent_AdminIdIn(java.util.Collection<String> adminIds);

    long countByEvent_AdminIdAndStatus(String adminId, ModerationStatus status);

    long countByEvent_AdminIdAndDeletedAtIsNotNull(String adminId);
}
