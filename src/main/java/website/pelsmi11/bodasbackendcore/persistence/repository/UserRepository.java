package website.pelsmi11.bodasbackendcore.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import website.pelsmi11.bodasbackendcore.persistence.model.User;
import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findFirstByCognitoId(String cognitoId);

    // ---- Ownership-scoped queries (admin caller = participants of administered events) ----

    @Query("""
            SELECT u FROM User u
            WHERE u.id IN (SELECT ep.user.id FROM EventParticipant ep WHERE ep.event.adminId = :adminId)
              AND (:role IS NULL OR u.role = :role)
            ORDER BY u.createdAt DESC
            """)
    Page<User> findAdminParticipants(@Param("adminId") String adminId, @org.springframework.lang.Nullable UserRole role, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.id IN (SELECT ep.user.id FROM EventParticipant ep WHERE ep.event.adminId = :adminId)
              AND (:role IS NULL OR u.role = :role)
              AND (LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY u.createdAt DESC
            """)
    Page<User> searchAdminParticipants(@Param("adminId") String adminId, @Param("q") String q,
                                       @org.springframework.lang.Nullable UserRole role, Pageable pageable);
}
