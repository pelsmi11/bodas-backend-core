package website.pelsmi11.bodasbackendcore.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import website.pelsmi11.bodasbackendcore.persistence.model.EventParticipant;
import website.pelsmi11.bodasbackendcore.persistence.model.EventParticipantId;

/**
 * Guest-to-event membership repository over the existing V4 table.
 * Idempotent upsert semantics (ON CONFLICT DO UPDATE last_accessed_at);
 * duplicates during reconciliation collapse to one row.
 */
public interface EventParticipantRepository extends JpaRepository<EventParticipant, EventParticipantId> {

    List<EventParticipant> findByUserId(Integer userId);

    Optional<EventParticipant> findByEventIdAndUserId(Integer eventId, Integer userId);

    boolean existsByEventIdAndUserId(Integer eventId, Integer userId);

    /**
     * Idempotent participation upsert: inserts on first access, touches
     * {@code last_accessed_at} on conflict (real event access only).
     */
    @Modifying
    @Query(value = """
            INSERT INTO event_participants (event_id, user_id)
            VALUES (:eventId, :userId)
            ON CONFLICT (event_id, user_id)
            DO UPDATE SET last_accessed_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int upsertParticipant(@Param("eventId") Integer eventId, @Param("userId") Integer userId);

    /** User IDs participating in events administered by the given admin (ownership scope). */
    @Query("SELECT DISTINCT ep.user.id FROM EventParticipant ep WHERE ep.event.adminId = :adminId")
    List<Integer> findParticipantUserIdsByAdminId(@Param("adminId") String adminId);
}
