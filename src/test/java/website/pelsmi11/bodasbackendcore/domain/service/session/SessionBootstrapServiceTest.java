package website.pelsmi11.bodasbackendcore.domain.service.session;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Reconciliation unit tests (data-model.md T1–T7) with stubbed repositories.
 */
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class SessionBootstrapServiceTest {

    private static final String SUB = "cognito-sub-b";
    private static final String SUB_A = "cognito-sub-A";
    private static final UUID GUEST = TestDataFactory.TEST_GUEST_UUID;

    @Mock
    private UserDeviceRepository userDeviceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventParticipantRepository eventParticipantRepository;
    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private SessionBootstrapService service;

    private User anonymousUser() {
        User user = new User();
        user.setId(100);
        user.setCognitoId(null);
        user.setRole(UserRole.GUEST);
        return user;
    }

    private User accountUser() {
        User user = TestDataFactory.user(50, SUB);
        user.setRole(UserRole.GUEST);
        return user;
    }

    private User accountA() {
        User user = TestDataFactory.user(300, "cognito-sub-A");
        user.setCognitoId("cognito-sub-A");
        return user;
    }

    private User accountB() {
        User user = TestDataFactory.user(200, SUB);
        user.setCognitoId(SUB);
        return user;
    }

    private UserDevice deviceFor(User user) {
        UserDevice device = TestDataFactory.userDevice(GUEST, user, false);
        device.setLastActive(OffsetDateTime.now());
        return device;
    }

    // ---- T1: promote anonymous row in place -------------------------------

    @Test
    void t1_anonymousDevice_newCognitoSub_promotesRowInPlace() {
        User anon = anonymousUser();
        UserDevice device = deviceFor(anon);
        when(userDeviceRepository.findById(GUEST)).thenReturn(Optional.of(device));
        when(userRepository.findFirstByCognitoId(SUB)).thenReturn(Optional.empty());
        when(userRepository.save(anon)).thenReturn(anon);
        when(photoRepository.findDistinctEventIdsByDeviceUuid(GUEST)).thenReturn(List.of(1, 2));

        SessionResponse response = service.reconcile(SUB, false, "new@example.com", GUEST, null);

        assertThat(response.getReconciliation().getAction()).isEqualTo("PROMOTED_ANONYMOUS_USER");
        assertThat(response.getReconciliation().isRotateGuestUuid()).isFalse();
        assertThat(device.getUser().getCognitoId()).isEqualTo(SUB);
        assertThat(device.getUser().getId()).isEqualTo(100);
        assertThat(device.getUser().getEmail()).isEqualTo("new@example.com");
        // participation upserts for the device's events
        verify(eventParticipantRepository).upsertParticipant(1, 100);
        verify(eventParticipantRepository).upsertParticipant(2, 100);
    }

    // ---- T2: existing account row → transactional transfer -----------------

    @Test
    void t2_anonymousDevice_existingSub_mergesIntoAccount() {
        User anon = anonymousUser();
        UserDevice device = deviceFor(anon);
        User account = accountUser();
        Event event = TestDataFactory.event(7, "ev777", true);

        when(userDeviceRepository.findById(GUEST)).thenReturn(Optional.of(device));
        when(userRepository.findFirstByCognitoId(SUB)).thenReturn(Optional.of(account));
        when(userRepository.findFirstByCognitoId(SUB)).thenReturn(Optional.of(account));
        when(userRepository.save(account)).thenReturn(account);
        when(userDeviceRepository.save(device)).thenReturn(device);
        when(eventParticipantRepository.findByUserId(100))
                .thenReturn(List.of(new website.pelsmi11.bodasbackendcore.persistence.model.EventParticipant(
                        event, anon, OffsetDateTime.now(), OffsetDateTime.now())));
        when(eventParticipantRepository.existsByEventIdAndUserId(7, 50)).thenReturn(false);

        SessionResponse response = service.reconcile(SUB, false, "acct@example.com", GUEST, null);

        assertThat(response.getReconciliation().getAction()).isEqualTo("MERGED_INTO_ACCOUNT");
        verify(photoRepository).transferPhotosBetweenUsers(100, 50);
        // account user row deleted after merge
        verify(userRepository).delete(anon);
    }

    // ---- T3: new device for existing account --------------------------------

    @Test
    void t3_existingAccount_freshDevice_linksDevice() {
        User account = accountUser();
        when(userDeviceRepository.findById(GUEST)).thenReturn(Optional.empty());
        when(userRepository.findFirstByCognitoId(SUB)).thenReturn(Optional.of(account));
        when(userRepository.save(account)).thenReturn(account);

        SessionResponse response = service.reconcile(SUB, false, null, GUEST, null);

        assertThat(response.getReconciliation().getAction()).isEqualTo("LINKED_DEVICE");
        verify(userDeviceRepository).save(any());
    }

    // ---- T4: A/B case — never transfer A's data ------------------------------

    @Test
    void t4_deviceBoundToA_accountB_returnsRotationRequested() {
        User userA = accountA();
        userA.setId(300);
        UserDevice device = deviceFor(userA);
        when(userDeviceRepository.findById(GUEST)).thenReturn(Optional.of(device));
        when(userRepository.findFirstByCognitoId(SUB)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(201);
            return u;
        });

        SessionResponse response = service.reconcile(SUB, false, null, GUEST, null);

        assertThat(response.getReconciliation().getAction()).isEqualTo("ROTATION_REQUESTED");
        assertThat(response.getReconciliation().isRotateGuestUuid()).isTrue();
        // no data of A transferred: device row untouched, photos untouched
        assertThat(device.getUser().getId()).isEqualTo(300);
        verify(photoRepository, org.mockito.Mockito.never()).transferPhotosBetweenUsers(
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    // ---- FR-012a: structural no-gallery assertion ----------------------------

    @org.junit.jupiter.api.Test
    void fr012a_bootstrapResponse_containsNoPhotosOrGalleryFields() throws Exception {
        User anon = anonymousUser();
        UserDevice device = deviceFor(anon);
        when(userDeviceRepository.findById(GUEST)).thenReturn(java.util.Optional.of(device));
        when(userRepository.findFirstByCognitoId(SUB)).thenReturn(java.util.Optional.empty());
        when(userRepository.save(anon)).thenReturn(anon);
        when(photoRepository.findDistinctEventIdsByDeviceUuid(GUEST)).thenReturn(List.of());

        website.pelsmi11.bodasbackendcore.domain.dto.session.SessionResponse response =
                service.reconcile(SUB, false, "e@example.com", GUEST, null);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        String json = mapper.writeValueAsString(response);
        String lower = json.toLowerCase();
        for (String forbidden : List.of("photo", "photos", "photoid", "s3key", "imageurl", "gallery")) {
            assertThat(lower).doesNotContain(forbidden);
        }
        // and the DTO type itself carries no photo-bearing fields
        assertThat(response.getClass().getDeclaredFields()).noneMatch(
                field -> field.getName().toLowerCase().contains("photo"));
    }

    // ---- T6: idempotent retry ------------------------------------------------

    @Test
    void t6_sameAccount_again_returnsNoChange() {
        User account = accountUser();
        UserDevice device = deviceFor(account);
        when(userDeviceRepository.findById(GUEST)).thenReturn(Optional.of(device));
        when(userRepository.findFirstByCognitoId(SUB)).thenReturn(Optional.of(account));
        when(userRepository.save(account)).thenReturn(account);
        when(userDeviceRepository.save(device)).thenReturn(device);

        SessionResponse response = service.reconcile(SUB, false, null, GUEST, null);

        assertThat(response.getReconciliation().getAction()).isEqualTo("NO_CHANGE");
        assertThat(device.getLastActive()).isNotNull();
    }

    // ---- T7: participation via eventToken ------------------------------------

    @Test
    void t7_validEventToken_upsertsParticipation() {
        User account = accountUser();
        UserDevice device = deviceFor(account);
        Event event = TestDataFactory.event(9, "evtoken9", true);
        when(userDeviceRepository.findById(GUEST)).thenReturn(Optional.of(device));
        when(userRepository.findFirstByCognitoId(SUB)).thenReturn(Optional.of(account));
        when(userRepository.save(account)).thenReturn(account);
        when(userDeviceRepository.save(device)).thenReturn(device);
        when(eventRepository.findFirstByToken("evtoken9")).thenReturn(Optional.of(event));
        when(eventParticipantRepository.existsByEventIdAndUserId(9, 50)).thenReturn(true);
        when(eventParticipantRepository.findByUserId(50)).thenReturn(List.of());

        SessionResponse response = service.reconcile(SUB, false, null, GUEST, "evtoken9");

        assertThat(response.getParticipation().isJoined()).isTrue();
        assertThat(response.getParticipation().isAlreadyMember()).isTrue();
        verify(eventParticipantRepository).upsertParticipant(9, 50);
    }

    @Test
    void t7_unknownEventToken_returns404() {
        User account = accountUser();
        UserDevice device = deviceFor(account);
        when(userDeviceRepository.findById(GUEST)).thenReturn(Optional.of(device));
        when(userRepository.findFirstByCognitoId(SUB)).thenReturn(Optional.of(account));
        when(userRepository.save(account)).thenReturn(account);
        when(userDeviceRepository.save(device)).thenReturn(device);
        when(eventRepository.findFirstByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reconcile(SUB, false, null, GUEST, "nope"))
                .isInstanceOf(website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException.class)
                .hasMessageContaining("Event not found");
    }

    // ---- Admin role mirroring (users.role written by trusted bootstrap) ------

    @Test
    void adminBootstrap_writesAdminMirrorToPromotedRow() {
        User anon = anonymousUser();
        UserDevice device = deviceFor(anon);
        when(userDeviceRepository.findById(GUEST)).thenReturn(Optional.of(device));
        when(userRepository.findFirstByCognitoId(SUB)).thenReturn(Optional.empty());
        when(userRepository.save(anon)).thenReturn(anon);
        when(photoRepository.findDistinctEventIdsByDeviceUuid(GUEST)).thenReturn(List.of());
        when(eventRepository.findByAdminIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        service.reconcile(SUB, true, null, GUEST, null);

        assertThat(device.getUser().getRole()).isEqualTo(UserRole.ADMIN);
    }
}
