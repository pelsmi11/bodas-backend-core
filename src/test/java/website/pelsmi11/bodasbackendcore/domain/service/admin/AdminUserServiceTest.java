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
import website.pelsmi11.bodasbackendcore.domain.dto.admin.UserRoleUpdateRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.persistence.model.User;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;
import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserDeviceRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDeviceRepository userDeviceRepository;
    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void findAllUsers_noFilters_callsFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(TestDataFactory.user()));
        when(userRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        Page<AdminUserDto> result = adminUserService.findAllUsers(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findAllUsers_withRole_callsRoleQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findByRoleOrderByCreatedAtDesc(UserRole.ADMIN, pageable)).thenReturn(page);

        adminUserService.findAllUsers(null, UserRole.ADMIN, pageable);

        verify(userRepository).findByRoleOrderByCreatedAtDesc(UserRole.ADMIN, pageable);
    }

    @Test
    void findAllUsers_withSearch_callsSearchQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc("hector", "hector", pageable))
                .thenReturn(page);

        adminUserService.findAllUsers("hector", null, pageable);

        verify(userRepository).findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc("hector", "hector", pageable);
    }

    @Test
    void findAllUsers_withRoleAndSearch_callsCombinedQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findByRoleAndSearch(UserRole.ADMIN, "hector", pageable)).thenReturn(page);

        adminUserService.findAllUsers("hector", UserRole.ADMIN, pageable);

        verify(userRepository).findByRoleAndSearch(UserRole.ADMIN, "hector", pageable);
    }

    @Test
    void findUserById_existing_returnsDto() {
        User user = TestDataFactory.user();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        AdminUserDto dto = adminUserService.findUserById(1);

        assertThat(dto.id()).isEqualTo(1);
        assertThat(dto.cognitoId()).isEqualTo("cognito-user-1");
    }

    @Test
    void findUserById_notFound_throws404() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.findUserById(99))
                .isInstanceOf(CustomErrorException.class)
                .hasMessage("User not found");
    }

    @Test
    void findDevicesByUser_returnsDevicePage() {
        User user = TestDataFactory.user();
        UserDevice device = TestDataFactory.userDevice();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserDevice> page = new PageImpl<>(List.of(device));
        when(userDeviceRepository.findByUserIdOrderByLastActiveDesc(1, pageable)).thenReturn(page);

        Page<AdminDeviceDto> result = adminUserService.findDevicesByUser(1, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).guestUuid()).isEqualTo(device.getGuestUuid());
    }

    @Test
    void findDevicesByUser_userNotFound_throws404() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.findDevicesByUser(99, PageRequest.of(0, 20)))
                .isInstanceOf(CustomErrorException.class);
    }

    @Test
    void blockDevices_blockedTrue_updatesAndReturnsList() {
        UUID uuid1 = TestDataFactory.TEST_GUEST_UUID;
        DeviceBlockRequest request = new DeviceBlockRequest();
        request.setGuestUuids(List.of(uuid1));
        request.setBlocked(true);
        UserDevice device = TestDataFactory.userDevice(uuid1, TestDataFactory.user(), true);
        when(userDeviceRepository.updateBlocked(eq(List.of(uuid1)), eq(true), any())).thenReturn(1);
        when(userDeviceRepository.findByGuestUuidIn(List.of(uuid1))).thenReturn(List.of(device));

        List<AdminDeviceDto> result = adminUserService.blockDevices(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).blocked()).isTrue();
        verify(userDeviceRepository).updateBlocked(eq(List.of(uuid1)), eq(true), any());
    }

    @Test
    void blockDevices_blockedNull_defaultsToTrue() {
        UUID uuid = TestDataFactory.TEST_GUEST_UUID;
        DeviceBlockRequest request = new DeviceBlockRequest();
        request.setGuestUuids(List.of(uuid));
        request.setBlocked(null);
        when(userDeviceRepository.updateBlocked(eq(List.of(uuid)), eq(true), any())).thenReturn(1);
        when(userDeviceRepository.findByGuestUuidIn(List.of(uuid))).thenReturn(List.of());

        adminUserService.blockDevices(request);

        verify(userDeviceRepository).updateBlocked(eq(List.of(uuid)), eq(true), any());
    }

    @Test
    void setRole_updatesAndReturnsDto() {
        User user = TestDataFactory.user();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setRole(UserRole.ADMIN);
        AdminUserDto dto = adminUserService.setRole(1, request);

        assertThat(dto.role()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void setRole_userNotFound_throws404() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.setRole(99, new UserRoleUpdateRequest()))
                .isInstanceOf(CustomErrorException.class);
    }
}
