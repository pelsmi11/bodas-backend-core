package website.pelsmi11.bodasbackendcore.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import website.pelsmi11.bodasbackendcore.persistence.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findFirstByCognitoId(String cognitoId);
}
