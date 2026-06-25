package website.pelsmi11.bodasbackendcore.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    Page<UserDevice> findByUserIdOrderByLastActiveDesc(Integer userId, Pageable pageable);

    List<UserDevice> findByGuestUuidIn(List<UUID> guestUuids);

    @Modifying
    @Query("UPDATE UserDevice d SET d.blocked = :blocked, d.blockedAt = :at WHERE d.guestUuid IN :uuids")
    int updateBlocked(@Param("uuids") List<UUID> uuids, @Param("blocked") boolean blocked, @Param("at") OffsetDateTime at);
}
