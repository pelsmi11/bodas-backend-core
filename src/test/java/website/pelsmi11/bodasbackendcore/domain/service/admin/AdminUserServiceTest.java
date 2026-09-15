package website.pelsmi11.bodasbackendcore.domain.service.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminDeviceDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminUserDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.DeviceBlockRequest;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.model.EventParticipant;
import website.pelsmi11.bodasbackendcore.persistence.model.User;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;
import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;
import website.pelsmi11.bodasbackendcore.persistence.repository.EventParticipantRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserDeviceRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Ownership semantics (spec 001 T025): user/device admin queries are restricted
 * to participants of events administered by the caller; setRole is RETIRED
 * (users.role is a bootstrap mirror only).
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final String SUB = TestDataFactory.TEST_ADMIN_SUB;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDeviceRepository userDeviceRepository;
    @Mock
    private EventParticipantRepository eventParticipantRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private List<EventParticipant> callerScope() {
        return List.of(participant(1, 10, SUB));
    }

    private EventParticipant participant(Integer eventId, Integer userId, String adminId) {
        Event event = TestDataFactory.event(eventId, "tok" + eventId, true);
        event.setAdminId(adminId);
        User user = TestDataFactory.user(userId, "cognito-" + userId);
        return new EventParticipant(event, user, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void findAllUsers_scopesToAdminParticipants() {
        User participant = TestDataFactory.user(10, "cognito-10");
        when(userRepository.findAdminParticipants(SUB, null, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(participant)));

        Page<AdminUserDto> result = adminUserService.findAllUsers(SUB, null, null, PAGEABLE);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findAllUsers_withSearch_usesSearchQuery() {
        when(userRepository.searchAdminParticipants(SUB, "test", null, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(TestDataFactory.user(10, "cognito-10"))));

        Page<AdminUserDto> result = adminUserService.findAllUsers(SUB, "test", null, PAGEABLE);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findUserById_participantOfAdminEvent_returns200() {
        User user = TestDataFactory.user(10, "cognito-10");
        when(userRepository.findById(10)).thenReturn(java.util.Optional.of(user));
        when(eventParticipantRepository.findByUserId(10)).thenReturn(callerScope());

        AdminUserDto dto = adminUserService.findUserById(SUB, 10);
        assertThat(dto.id()).isEqualTo(10);
    }

    @Test
    void findUserById_nonParticipant_returns404() {
        User user = TestDataFactory.user(99, "cognito-99");
        when(userRepository.findById(99)).thenReturn(java.util.Optional.of(user));
        when(eventParticipantRepository.findByUserId(99)).thenReturn(List.of());

        assertThatThrownBy(() -> adminUserService.findUserById(SUB, 99))
                .isInstanceOf(website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void findDevicesByUser_participant_returnsDevices() {
        User user = TestDataFactory.user(10, "cognito-10");
        when(userRepository.findById(10)).thenReturn(java.util.Optional.of(user));
        when(eventParticipantRepository.findByUserId(10)).thenReturn(callerScope());
        when(userDeviceRepository.findByUserIdOrderByLastActiveDesc(10, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(TestDataFactory.userDevice())));

        Page<AdminDeviceDto> result = adminUserService.findDevicesByUser(SUB, 10, PAGEABLE);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findDevicesByUser_nonParticipant_returns404() {
        when(userRepository.findById(77)).thenReturn(java.util.Optional.of(TestDataFactory.user(77, "cognito-77")));
        when(eventParticipantRepository.findByUserId(77)).thenReturn(List.of());

        assertThatThrownBy(() -> adminUserService.findDevicesByUser(SUB, 77, PAGEABLE))
                .isInstanceOf(website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException.class);
    }

    @Test
    void blockDevices_filtersDevicesOutsideAdminScope() {
        User inScopeUser = TestDataFactory.user(10, "cognito-10");
        User outScopeUser = TestDataFactory.user(99, "cognito-99");
        UserDevice inScope = TestDataFactory.userDevice(UUID.fromString("11111111-1111-1111-1111-111111111111"), inScopeUser, false);
        UserDevice outScope = TestDataFactory.userDevice(UUID.fromString("22222222-2222-2222-2222-222222222222"), outScopeUser, false);
        when(userDeviceRepository.findByGuestUuidIn(any()))
                .thenReturn(List.of(inScope, outScope));
        when(eventParticipantRepository.findByUserId(10))
                .thenReturn(callerScope());
        when(eventParticipantRepository.findByUserId(99))
                .thenReturn(java.util.List.of());
        when(userDeviceRepository.findByGuestUuidIn(java.util.List.of(
                java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"))))
                .thenReturn(java.util.List.of(inScope));
        DeviceBlockRequest request = new DeviceBlockRequest();
        request.setGuestUuids(java.util.List.of(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222")));

        List<AdminDeviceDto> result = adminUserService.blockDevices(SUB, request);

        // only the in-scope device was blocked; foreign devices filtered out
        assertThat(result).hasSize(1);
    }

    @Test
    void blockDevices_emptyScope_returnsEmpty() {
        when(userDeviceRepository.findByGuestUuidIn(any())).thenReturn(List.of());
        DeviceBlockRequest request = new DeviceBlockRequest();
        request.setGuestUuids(java.util.List.of(UUID.fromString("33333333-3333-3333-3333-333333333333")));

        List<AdminDeviceDto> result = adminUserService.blockDevices(SUB, request);
        assertThat(result).isEmpty();
    }
}
