package website.pelsmi11.bodasbackendcore.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;

import java.util.UUID;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {
}
