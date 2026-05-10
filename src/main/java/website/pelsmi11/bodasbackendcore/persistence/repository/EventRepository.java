package website.pelsmi11.bodasbackendcore.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Integer> {

    Optional<Event> findFirstByToken(String token);
}
