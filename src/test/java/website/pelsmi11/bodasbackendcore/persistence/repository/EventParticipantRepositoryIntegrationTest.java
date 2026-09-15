package website.pelsmi11.bodasbackendcore.persistence.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.model.EventParticipant;
import website.pelsmi11.bodasbackendcore.persistence.model.EventStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.model.User;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;
import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REAL JPA integration tests over a PostgreSQL Testcontainers instance with
 * Flyway V1–V4 applied. NEVER touches the shared Neon main branch.
 * Skipped automatically when Docker is unavailable (BLOCKED / NOT RUN branch).
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class EventParticipantRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final UUID DEVICE_A = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID DEVICE_B = UUID.fromString("b0000000-0000-0000-0000-000000000002");

    @Autowired
    private EventParticipantRepository eventParticipantRepository;
    @Autowired
    private PhotoRepository photoRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserDeviceRepository userDeviceRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EntityManager entityManager;

    private User saveUser(String cognitoId) {
        User user = new User();
        user.setCognitoId(cognitoId);
        user.setRole(UserRole.GUEST);
        user.setCreatedAt(OffsetDateTime.now());
        return userRepository.save(user);
    }

    private Event saveEvent(String adminId, String token) {
        Event event = new Event();
        event.setName("Boda " + token);
        event.setToken(token);
        event.setAdminId(adminId);
        event.setStatus(EventStatus.ACTIVE);
        event.setIsActive(true);
        return eventRepository.save(event);
    }

    private UserDevice saveDevice(UUID guestUuid, User user) {
        UserDevice device = new UserDevice();
        device.setGuestUuid(guestUuid);
        device.setUser(user);
        device.setLastActive(OffsetDateTime.now());
        return userDeviceRepository.save(device);
    }

    private Photo savePhoto(Event event, User user, UserDevice device, String key) {
        Photo photo = new Photo();
        photo.setEvent(event);
        photo.setUser(user);
        photo.setDevice(device);
        photo.setS3Key(key);
        photo.setUploadedAt(OffsetDateTime.now());
        return photoRepository.save(photo);
    }

    @Test
    void upsertParticipant_insertsThenDedupesTouchingLastAccessedAt() {
        User user = saveUser("upsert-user");
        Event event = saveEvent("admin-sub", "ev-upsert");

        eventParticipantRepository.upsertParticipant(event.getId(), user.getId());
        eventParticipantRepository.upsertParticipant(event.getId(), user.getId());

        long rows = eventParticipantRepository.count();
        assertThat(rows).isEqualTo(1L); // composite PK dedupe
        EventParticipant participant = eventParticipantRepository.findByEventIdAndUserId(event.getId(), user.getId())
                .orElseThrow();
        assertThat(participant.getLastAccessedAt()).isNotNull();
    }

    @Test
    void transferPhotosBetweenUsers_repointsAttribution() {
        String adminId = "admin-transfer";
        Event event = saveEvent(adminId, "ev-transfer");
        User fromUser = saveUser("anon-from");
        User toUser = saveUser("account-to");
        UserDevice device = saveDevice(DEVICE_A, fromUser);
        savePhoto(event, fromUser, device, "k/from/1.jpg");

        photoRepository.transferPhotosBetweenUsers(fromUser.getId(), toUser.getId());
        entityManager.flush();
        entityManager.clear();

        Photo moved = photoRepository.findAll().get(0);
        assertThat(moved.getUser().getId()).isEqualTo(toUser.getId());
        // device reference preserved (photos remain traceable, device re-pointed by caller)
        assertThat(moved.getDevice()).isNotNull();
    }

    @Test
    void findDistinctEventIdsByDeviceUuid_returnsDistinctEventIds() {
        String adminId = "admin-distinct";
        Event eventA = saveEvent(adminId, "ev-dist-a");
        Event eventB = saveEvent(adminId, "ev-dist-b");
        User user = saveUser("device-user");
        UserDevice device = saveDevice(DEVICE_A, user);
        savePhoto(eventA, user, device, "k/a/1.jpg");
        savePhoto(eventA, user, device, "k/a/2.jpg");
        savePhoto(eventB, user, device, "k/b/1.jpg");

        List<Integer> ids = photoRepository.findDistinctEventIdsByDeviceUuid(DEVICE_A);

        assertThat(ids).containsExactlyInAnyOrder(eventA.getId(), eventB.getId());
    }

    @Test
    void findAdminParticipants_returnsOnlyParticipantsOfAdminEvents() {
        String caller = "admin-caller";
        String other = "admin-other";
        Event ownEvent = saveEvent(caller, "ev-own");
        Event foreignEvent = saveEvent(other, "ev-foreign");
        User participant = saveUser("participant-1");
        participant.setName("Test Participant");
        userRepository.save(participant);
        User outsider = saveUser("outsider-1");
        eventParticipantRepository.save(new EventParticipant(
                ownEvent, participant, OffsetDateTime.now(), OffsetDateTime.now()));
        eventParticipantRepository.save(new EventParticipant(
                foreignEvent, outsider, OffsetDateTime.now(), OffsetDateTime.now()));

        PageRequest page = PageRequest.of(0, 20);
        List<Integer> ids = eventParticipantRepository.findParticipantUserIdsByAdminId(caller);
        assertThat(ids).containsExactly(participant.getId());

        var result = userRepository.findAdminParticipants(caller, null, page);
        assertThat(result.getContent()).extracting(User::getId).containsExactly(participant.getId());

        var searched = userRepository.searchAdminParticipants(caller, "Test", null, page);
        assertThat(searched.getContent()).extracting(User::getId).containsExactly(participant.getId());
    }

    @Test
    void ownershipQueries_matchAdminId() {
        String caller = "admin-owner";
        String other = "admin-foreign";
        Event ownEvent = saveEvent(caller, "ev-owner");
        User owner = saveUser("owner-user");
        UserDevice device = saveDevice(DEVICE_B, owner);
        Photo own = savePhoto(ownEvent, owner, device, "k/own/1.jpg");

        Event foreignEvent = saveEvent(other, "ev-foreign");
        Photo foreignPhoto = savePhoto(foreignEvent, owner, device, "k/foreign/1.jpg");
        foreignPhoto.setEvent(foreignEvent);
        photoRepository.save(foreignPhoto);

        assertThat(photoRepository.existsByIdAndEvent_AdminId(own.getId(), caller)).isTrue();
        assertThat(photoRepository.existsByIdAndEvent_AdminId(own.getId(), other)).isFalse();
        assertThat(eventParticipantRepository.existsByEventIdAndUserId(ownEvent.getId(), owner.getId())).isFalse();
    }
}
