package website.pelsmi11.bodasbackendcore.domain.service.photo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.persistence.model.User;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserDeviceRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestIdentityServiceTest {

    private static final UUID GUEST_UUID = TestDataFactory.TEST_GUEST_UUID;
    private static final String COGNITO_ID = "cognito-user-1";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private GuestIdentityService guestIdentityService;

    @Test
    void parseGuestUuid_invalidFormat_throws400() {
        assertThatThrownBy(() -> guestIdentityService.parseGuestUuid("not-a-uuid"))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("UUID");
    }

    @Test
    void parseGuestUuid_validFormat_returnsUuid() {
        UUID result = guestIdentityService.parseGuestUuid(GUEST_UUID.toString());

        assertThat(result).isEqualTo(GUEST_UUID);
    }

    @Test
    void resolveAnonymous_newDevice_createsUserAndDevice() {
        when(userDeviceRepository.findById(GUEST_UUID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1);
            return u;
        });
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDevice result = guestIdentityService.resolveUserDevice(GUEST_UUID.toString(), null);

        assertThat(result.getGuestUuid()).isEqualTo(GUEST_UUID);
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getLastActive()).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(userDeviceRepository).save(any(UserDevice.class));
    }

    @Test
    void resolveAnonymous_existingDevice_touchesAndReturns() {
        UserDevice existing = TestDataFactory.userDevice();
        when(userDeviceRepository.findById(GUEST_UUID)).thenReturn(Optional.of(existing));
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDevice result = guestIdentityService.resolveUserDevice(GUEST_UUID.toString(), null);

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
        verify(userDeviceRepository).save(existing);
    }

    @Test
    void resolveAnonymous_blockedDevice_throws403() {
        UserDevice blocked = TestDataFactory.userDevice(GUEST_UUID, TestDataFactory.user(), true);
        when(userDeviceRepository.findById(GUEST_UUID)).thenReturn(Optional.of(blocked));

        assertThatThrownBy(() -> guestIdentityService.resolveUserDevice(GUEST_UUID.toString(), null))
                .isInstanceOf(CustomErrorException.class)
                .hasMessage("Device blocked");
    }

    @Test
    void resolveAuthenticated_newUserNewDevice_createsBoth() {
        when(userRepository.findFirstByCognitoId(COGNITO_ID)).thenReturn(Optional.empty());
        when(userDeviceRepository.findById(GUEST_UUID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1);
            return u;
        });
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDevice result = guestIdentityService.resolveUserDevice(GUEST_UUID.toString(), COGNITO_ID);

        assertThat(result.getUser().getCognitoId()).isEqualTo(COGNITO_ID);
        verify(userRepository).save(any(User.class));
        verify(userDeviceRepository).save(any(UserDevice.class));
    }

    @Test
    void resolveAuthenticated_existingUserNewDevice_linksDevice() {
        User existingUser = TestDataFactory.user(1, COGNITO_ID);
        when(userRepository.findFirstByCognitoId(COGNITO_ID)).thenReturn(Optional.of(existingUser));
        when(userDeviceRepository.findById(GUEST_UUID)).thenReturn(Optional.empty());
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDevice result = guestIdentityService.resolveUserDevice(GUEST_UUID.toString(), COGNITO_ID);

        assertThat(result.getUser()).isSameAs(existingUser);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resolveAuthenticated_existingUserExistingDeviceSameUser_touches() {
        User existingUser = TestDataFactory.user(1, COGNITO_ID);
        UserDevice existingDevice = TestDataFactory.userDevice(GUEST_UUID, existingUser, false);
        when(userRepository.findFirstByCognitoId(COGNITO_ID)).thenReturn(Optional.of(existingUser));
        when(userDeviceRepository.findById(GUEST_UUID)).thenReturn(Optional.of(existingDevice));
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDevice result = guestIdentityService.resolveUserDevice(GUEST_UUID.toString(), COGNITO_ID);

        assertThat(result).isSameAs(existingDevice);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resolveAuthenticated_deviceLinkedToOtherUser_throws409() {
        User cognitoUser = TestDataFactory.user(1, COGNITO_ID);
        User otherUser = TestDataFactory.user(2, "other-cognito");
        UserDevice device = TestDataFactory.userDevice(GUEST_UUID, otherUser, false);
        when(userRepository.findFirstByCognitoId(COGNITO_ID)).thenReturn(Optional.of(cognitoUser));
        when(userDeviceRepository.findById(GUEST_UUID)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> guestIdentityService.resolveUserDevice(GUEST_UUID.toString(), COGNITO_ID))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("another user");
    }

    @Test
    void resolveAuthenticated_migrationAnonymousToAuth_assignsCognitoToDeviceUser() {
        User anonymousUser = TestDataFactory.user(1, null);
        UserDevice device = TestDataFactory.userDevice(GUEST_UUID, anonymousUser, false);
        when(userRepository.findFirstByCognitoId(COGNITO_ID)).thenReturn(Optional.empty());
        when(userDeviceRepository.findById(GUEST_UUID)).thenReturn(Optional.of(device));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDevice result = guestIdentityService.resolveUserDevice(GUEST_UUID.toString(), COGNITO_ID);

        assertThat(result.getUser().getCognitoId()).isEqualTo(COGNITO_ID);
        verify(userRepository).save(anonymousUser);
    }

    @Test
    void resolveAuthenticated_deviceBelongsToDifferentCognito_throws409() {
        User deviceUser = TestDataFactory.user(1, "existing-cognito");
        UserDevice device = TestDataFactory.userDevice(GUEST_UUID, deviceUser, false);
        when(userRepository.findFirstByCognitoId(COGNITO_ID)).thenReturn(Optional.empty());
        when(userDeviceRepository.findById(GUEST_UUID)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> guestIdentityService.resolveUserDevice(GUEST_UUID.toString(), COGNITO_ID))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("different account");
    }

    @Test
    void resolveAuthenticated_blockedDevice_throws403() {
        UserDevice blocked = TestDataFactory.userDevice(GUEST_UUID, TestDataFactory.user(), true);
        when(userRepository.findFirstByCognitoId(COGNITO_ID)).thenReturn(Optional.empty());
        when(userDeviceRepository.findById(GUEST_UUID)).thenReturn(Optional.of(blocked));

        assertThatThrownBy(() -> guestIdentityService.resolveUserDevice(GUEST_UUID.toString(), COGNITO_ID))
                .isInstanceOf(CustomErrorException.class)
                .hasMessage("Device blocked");
    }

    @Test
    void resolveUserDevice_uuidOverload_anonymous_works() {
        when(userDeviceRepository.findById(GUEST_UUID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1);
            return u;
        });
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDevice result = guestIdentityService.resolveUserDevice(GUEST_UUID, null);

        assertThat(result.getGuestUuid()).isEqualTo(GUEST_UUID);
    }
}
