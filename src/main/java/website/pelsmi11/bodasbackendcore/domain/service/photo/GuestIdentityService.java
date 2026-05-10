package website.pelsmi11.bodasbackendcore.domain.service.photo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.persistence.model.User;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserDeviceRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resolves and links anonymous/authenticated identity using guest UUID plus optional Cognito subject.
 */
@Component
public class GuestIdentityService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;

    public GuestIdentityService(UserRepository userRepository, UserDeviceRepository userDeviceRepository) {
        this.userRepository = userRepository;
        this.userDeviceRepository = userDeviceRepository;
    }

    /**
     * Resolves the user device for a request. Anonymous users are lazily registered.
     * Authenticated users are linked/merged by Cognito subject.
     */
    public UserDevice resolveUserDevice(String guestIdRaw, String cognitoId) {
        UUID guestUuid = parseGuestUuid(guestIdRaw);
        if (cognitoId == null || cognitoId.isBlank()) {
            return resolveAnonymousDevice(guestUuid);
        }
        return resolveAuthenticatedDevice(guestUuid, cognitoId);
    }

    /**
     * Overload for already-parsed guest UUID.
     */
    public UserDevice resolveUserDevice(UUID guestUuid, String cognitoId) {
        if (cognitoId == null || cognitoId.isBlank()) {
            return resolveAnonymousDevice(guestUuid);
        }
        return resolveAuthenticatedDevice(guestUuid, cognitoId);
    }

    /**
     * Parses guest UUID and throws a business error when the format is invalid.
     */
    public UUID parseGuestUuid(String guestIdRaw) {
        try {
            return UUID.fromString(guestIdRaw);
        } catch (IllegalArgumentException exception) {
            throw CustomErrorException.handlerCustomError("guestId no tiene formato UUID valido", HttpStatus.BAD_REQUEST);
        }
    }

    private UserDevice resolveAnonymousDevice(UUID guestUuid) {
        UserDevice existing = userDeviceRepository.findById(guestUuid).orElse(null);
        if (existing != null) {
            touchDevice(existing);
            return userDeviceRepository.save(existing);
        }

        User anonymousUser = new User();
        User savedUser = userRepository.save(anonymousUser);
        return saveNewDevice(guestUuid, savedUser);
    }

    private UserDevice resolveAuthenticatedDevice(UUID guestUuid, String cognitoId) {
        User userByCognito = userRepository.findFirstByCognitoId(cognitoId).orElse(null);
        UserDevice device = userDeviceRepository.findById(guestUuid).orElse(null);

        if (userByCognito != null) {
            if (device == null) {
                return saveNewDevice(guestUuid, userByCognito);
            }
            if (!device.getUser().getId().equals(userByCognito.getId())) {
                throw CustomErrorException.handlerCustomError("Este dispositivo ya esta vinculado a otro usuario", HttpStatus.CONFLICT);
            }
            touchDevice(device);
            return userDeviceRepository.save(device);
        }

        if (device != null) {
            User deviceUser = device.getUser();
            if (deviceUser.getCognitoId() != null && !deviceUser.getCognitoId().isBlank() && !deviceUser.getCognitoId().equals(cognitoId)) {
                throw CustomErrorException.handlerCustomError("El dispositivo pertenece a una cuenta diferente", HttpStatus.CONFLICT);
            }
            deviceUser.setCognitoId(cognitoId);
            userRepository.save(deviceUser);
            touchDevice(device);
            return userDeviceRepository.save(device);
        }

        User newUser = new User();
        newUser.setCognitoId(cognitoId);
        User savedUser = userRepository.save(newUser);
        return saveNewDevice(guestUuid, savedUser);
    }

    private UserDevice saveNewDevice(UUID guestUuid, User user) {
        UserDevice userDevice = new UserDevice();
        userDevice.setGuestUuid(guestUuid);
        userDevice.setUser(user);
        touchDevice(userDevice);
        return userDeviceRepository.save(userDevice);
    }

    private void touchDevice(UserDevice userDevice) {
        userDevice.setLastActive(OffsetDateTime.now());
    }
}
