package website.pelsmi11.bodasbackendcore.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.model.EventStatus;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Integer> {

    Optional<Event> findFirstByToken(String token);

    boolean existsByToken(String token);

    Page<Event> findByAdminIdOrderByCreatedAtDesc(String adminId, Pageable pageable);

    Page<Event> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Event> findByStatusOrderByCreatedAtDesc(EventStatus status, Pageable pageable);

    Page<Event> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String search, Pageable pageable);

    Page<Event> findByNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(String search, EventStatus status, Pageable pageable);

    // ---- Ownership-scoped queries (admin caller = events.admin_id) ----------

    Page<Event> findByAdminIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(String adminId, String search, Pageable pageable);

    Page<Event> findByAdminIdAndStatusOrderByCreatedAtDesc(String adminId, EventStatus status, Pageable pageable);

    Page<Event> findByAdminIdAndNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(String adminId, String search, EventStatus status, Pageable pageable);
}
