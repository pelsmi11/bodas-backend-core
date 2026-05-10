package website.pelsmi11.bodasbackendcore.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    Optional<Photo> findFirstByS3Key(String s3Key);

    List<Photo> findByEventTokenAndStatusOrderByUploadedAtDesc(String eventToken, ModerationStatus status);
}
