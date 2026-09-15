package website.pelsmi11.bodasbackendcore.domain.service.session;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionEventDto;
import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionParticipationDto;
import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionReconciliationDto;
import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionUserDto;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.model.User;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;
import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;
import website.pelsmi11.bodasbackendcore.persistence.repository.EventParticipantRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.EventRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserDeviceRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserRepository;
import website.pelsmi11.bodasbackendcore.web.auth.CognitoGroupsAuthoritiesConverter;

/**
 * Idempotent, transactional reconciliation (data-model.md transitions T1–T7).
 *
 * - T1 PROMOTED_ANONYMOUS_USER: device belongs to an anonymous users row and no
 *   row exists for the Cognito sub → the anonymous row is promoted IN PLACE
 *   (cognito_id, email, role mirror derived from token groups, last_login_at).
 *   Device keeps its user_id; photos keep user_id; no second user row.
 * - T2 MERGED_INTO_ACCOUNT: a DB row already exists for the sub → transactional
 *   transfer of photos/devices/participations from the anonymous row; duplicates
 *   deduped; anonymous row deleted.
 * - T3 LINKED_DEVICE: account authenticates from another/new device.
 * - T4 ROTATION_REQUESTED: device bound to account A, account B authenticates →
 *   NO data of A is transferred; normal flow returns 200 with
 *   rotateGuestUuid=true; 409 is reserved for an explicit transfer request in a
 *   non-rotatable context (contracts/auth-session-api.md).
 * - T6 NO_CHANGE idempotent retries.
 * - T7 participation upsert via eventToken (dedupe by PK).
 *
 * Timestamps: user_devices.last_active updates on EVERY bootstrap;
 * event_participants.last_accessed_at ONLY on real event access (valid
 * eventToken or participation registration).
 */
@Service
public class SessionBootstrapService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final PhotoRepository photoRepository;

    public SessionBootstrapService(
            UserDeviceRepository userDeviceRepository,
            UserRepository userRepository,
            EventRepository eventRepository,
            EventParticipantRepository eventParticipantRepository,
            PhotoRepository photoRepository) {
        this.userDeviceRepository = userDeviceRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.photoRepository = photoRepository;
    }

    /**
     * Reconciliation entry point; runs INSIDE the caller's transaction (the
     * GetUser email lookup has already happened by the time this is called).
     */
    @Transactional
    public SessionResponse reconcile(
            String sub,
            boolean isAdmin,
            String email,
            UUID guestUuid,
            String eventToken) {

        // Guest identity is preserved as-is: the public contract name is
        // guestUuid; legacy internal naming must not leak into the API.
        UserDevice device = userDeviceRepository.findById(guestUuid).orElse(null);
        User accountUser = userRepository.findFirstByCognitoId(sub).orElse(null);

        String role = isAdmin ? "ADMIN" : "GUEST";
        SessionReconciliationDto reconciliation;
        User sessionUser;

        if (device == null) {
            // T3-like: authenticated principal on a fresh device
            sessionUser = ensureAccountUser(accountUser, sub, email);
            createDevice(guestUuid, sessionUser);
            reconciliation = new SessionReconciliationDto("LINKED_DEVICE", false);
        } else {
            User deviceUser = device.getUser();
            boolean deviceAnonymous = deviceUser.getCognitoId() == null || deviceUser.getCognitoId().isBlank();

            if (deviceAnonymous) {
                if (accountUser == null) {
                    // T1: promote the anonymous row in place (no second user row,
                    // no photo re-pointing needed).
                    deviceUser.setCognitoId(sub);
                    if (email != null && deviceUser.getEmail() == null) {
                        deviceUser.setEmail(email);
                    }
                    deviceUser.setRole(isAdmin
                            ? UserRole.ADMIN
                            : UserRole.GUEST);
                    deviceUser.setLastLoginAt(OffsetDateTime.now());
                    userRepository.save(deviceUser);
                    touch(device);
                    upsertParticipationsForDeviceEvents(guestUuid, deviceUser);
                    sessionUser = deviceUser;
                    reconciliation = new SessionReconciliationDto("PROMOTED_ANONYMOUS_USER", false);
                } else {
                    // T2: transactional transfer from the anonymous row to the account row.
                    mergeAnonymousIntoAccount(guestUuid, deviceUser, accountUser);
                    touch(device);
                    sessionUser = userRepository.findFirstByCognitoId(sub).orElse(accountUser);
                    reconciliation = new SessionReconciliationDto("MERGED_INTO_ACCOUNT", false);
                }
            } else if (sub.equals(deviceUser.getCognitoId())) {
                // T6: same account again — idempotent no-op (last_active only).
                touch(device);
                sessionUser = userRepository.findFirstByCognitoId(sub).orElse(deviceUser);
                reconciliation = new SessionReconciliationDto("NO_CHANGE", false);
            } else {
                // T4: device bound to account A, account B authenticates — NEVER
                // transfer A's data; B proceeds authenticated; frontend rotates
                // guestUuid after logout.
                sessionUser = ensureAccountUser(accountUser, sub, email);
                reconciliation = new SessionReconciliationDto("ROTATION_REQUESTED", true);
            }
        }

        SessionParticipationDto participation = null;
        if (eventToken != null && !eventToken.isBlank()) {
            participation = registerParticipation(eventToken, sessionUser);
        }

        return buildResponse(sessionUser, role, reconciliation, participation);
    }

    /** Ensures the account row exists for a Cognito sub (creates when missing). */
    private User ensureAccountUser(User accountUser, String sub, String email) {
        if (accountUser != null) {
            if (email != null && accountUser.getEmail() == null) {
                accountUser.setEmail(email);
            }
            accountUser.setLastLoginAt(OffsetDateTime.now());
            return userRepository.save(accountUser);
        }
        User created = new User();
        created.setCognitoId(sub);
        if (email != null) {
            created.setEmail(email);
        }
        created.setLastLoginAt(OffsetDateTime.now());
        return userRepository.save(created);
    }

    private void touch(UserDevice device) {
        // user_devices.last_active changes on EVERY bootstrap
        device.setLastActive(OffsetDateTime.now());
        userDeviceRepository.save(device);
    }

    private void createDevice(UUID guestUuid, User user) {
        UserDevice device = new UserDevice();
        device.setGuestUuid(guestUuid);
        device.setUser(user);
        device.setLastActive(OffsetDateTime.now());
        userDeviceRepository.save(device);
    }

    /** T1 helper: participations from events where the device uploaded photos. */
    private void upsertParticipationsForDeviceEvents(UUID guestUuid, User user) {
        List<Integer> eventIds = photoRepository.findDistinctEventIdsByDeviceUuid(guestUuid);
        for (Integer eventId : eventIds) {
            eventParticipantRepository.upsertParticipant(eventId, user.getId());
        }
    }

    /** T2 helper: transactional transfer, never touching A's data. */
    private void mergeAnonymousIntoAccount(UUID guestUuid, User anonymousUser, User accountUser) {
        // 1. Photos created anonymously re-point to the account user.
        photoRepository.transferPhotosBetweenUsers(anonymousUser.getId(), accountUser.getId());
        // 2. Device row re-points to the account user.
        anonymousUser.getDevices().forEach(device -> {
            device.setUser(accountUser);
            userDeviceRepository.save(device);
        });
        // 3. Participations merge (dedupe by composite PK).
        List<website.pelsmi11.bodasbackendcore.persistence.model.EventParticipant> anonParticipations =
                eventParticipantRepository.findByUserId(anonymousUser.getId());
        for (website.pelsmi11.bodasbackendcore.persistence.model.EventParticipant participant : anonParticipations) {
            if (!eventParticipantRepository.existsByEventIdAndUserId(participant.getEventId(), accountUser.getId())) {
                eventParticipantRepository.save(new website.pelsmi11.bodasbackendcore.persistence.model.EventParticipant(
                        participant.getEvent(), accountUser, participant.getJoinedAt(), participant.getLastAccessedAt()));
            }
        }
        // 4. Drop the merged anonymous duplicate row (photos/devices already moved).
        try {
            userRepository.delete(anonymousUser);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Retain anonymous row when references remain; attribution is already
            // complete. Never fail the bootstrap for a duplicate-merge retry.
        }
    }

    /** T7: idempotent participation registration for a valid event token. */
    private SessionParticipationDto registerParticipation(String eventToken, User user) {
        Event event = eventRepository.findFirstByToken(eventToken)
                .filter(e -> Boolean.TRUE.equals(e.getIsActive()))
                .orElseThrow(() -> CustomErrorException.handlerCustomError("Event not found", org.springframework.http.HttpStatus.NOT_FOUND));
        boolean alreadyMember = eventParticipantRepository.existsByEventIdAndUserId(event.getId(), user.getId());
        eventParticipantRepository.upsertParticipant(event.getId(), user.getId());
        return new SessionParticipationDto(eventToken, true, alreadyMember);
    }

    private SessionResponse buildResponse(
            User user, String role, SessionReconciliationDto reconciliation, SessionParticipationDto participation) {
        List<SessionEventDto> events = new ArrayList<>();
        eventParticipantRepository.findByUserId(user.getId()).forEach(participant -> {
            Event event = participant.getEvent();
            if (event != null) {
                events.add(new SessionEventDto(event.getId(), event.getName(), event.getToken(), "PARTICIPANT"));
            }
        });
        if ("ADMIN".equals(role)) {
            eventRepository.findByAdminIdOrderByCreatedAtDesc(user.getCognitoId(), PageRequest.of(0, 100))
                    .forEach(event -> {
                        if (events.stream().noneMatch(e -> e.getEventId().equals(event.getId()))) {
                            events.add(new SessionEventDto(event.getId(), event.getName(), event.getToken(), "ADMIN"));
                        }
                    });
        }

        SessionResponse response = new SessionResponse();
        response.setUser(new SessionUserDto(user.getId(), user.getEmail()));
        response.setRole(role);
        response.setEvents(events);
        response.setReconciliation(reconciliation);
        response.setParticipation(participation);
        return response;
    }
}
