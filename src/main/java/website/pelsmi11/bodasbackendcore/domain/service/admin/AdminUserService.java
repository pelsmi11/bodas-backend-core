package website.pelsmi11.bodasbackendcore.domain.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminDeviceDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminUserDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.DeviceBlockRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.persistence.model.User;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;
import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserDeviceRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Administrative operations for users and device management.
 */
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final website.pelsmi11.bodasbackendcore.persistence.repository.EventParticipantRepository eventParticipantRepository;

    public AdminUserService(
            UserRepository userRepository,
            UserDeviceRepository userDeviceRepository,
            website.pelsmi11.bodasbackendcore.persistence.repository.EventParticipantRepository eventParticipantRepository
    ) {
        this.userRepository = userRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.eventParticipantRepository = eventParticipantRepository;
    }

    /**
     * Ownership scope: only users participating in events administered by the
     * caller (never global user listings — FR-015/FR-020).
     */
    @Transactional(readOnly = true)
    public Page<AdminUserDto> findAllUsers(String adminSub, String search, UserRole role, Pageable pageable) {
        Page<User> page;
        if (search != null && !search.isBlank()) {
            page = userRepository.searchAdminParticipants(adminSub, search, role, pageable);
        } else {
            page = userRepository.findAdminParticipants(adminId(adminSub), role, pageable);
        }
        return page.map(this::toUserDto);
    }

    @Transactional(readOnly = true)
    public AdminUserDto findUserById(String adminSub, Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> CustomErrorException.handlerCustomError("User not found", HttpStatus.NOT_FOUND));
        boolean participates = eventParticipantRepository.findByUserId(id).stream()
                .anyMatch(participant -> adminSub.equals(participant.getEvent().getAdminId()));
        if (!participates) {
            throw CustomErrorException.handlerCustomError("User not found", HttpStatus.NOT_FOUND);
        }
        return toUserDto(user);
    }

    @Transactional(readOnly = true)
    public Page<AdminDeviceDto> findDevicesByUser(String adminSub, Integer userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomErrorException.handlerCustomError("User not found", HttpStatus.NOT_FOUND));
        boolean participates = eventParticipantRepository.findByUserId(userId).stream()
                .anyMatch(participant -> adminSub.equals(participant.getEvent().getAdminId()));
        if (!participates) {
            throw CustomErrorException.handlerCustomError("User not found", HttpStatus.NOT_FOUND);
        }
        return userDeviceRepository.findByUserIdOrderByLastActiveDesc(userId, pageable)
                .map(this::toDeviceDto);
    }

    /** Device block/unblock restricted to devices of participants of administered events. */
    @Transactional
    public List<AdminDeviceDto> blockDevices(String adminSub, DeviceBlockRequest request) {
        boolean blocked = request.getBlocked() == null || request.getBlocked();
        OffsetDateTime at = blocked ? OffsetDateTime.now() : null;
        List<UUID> allowed = userDeviceRepository.findByGuestUuidIn(request.getGuestUuids()).stream()
                .filter(device -> eventParticipantRepository.findByUserId(device.getUser().getId()).stream()
                        .anyMatch(participant -> adminSub.equals(participant.getEvent().getAdminId())))
                .map(website.pelsmi11.bodasbackendcore.persistence.model.UserDevice::getGuestUuid)
                .toList();
        if (allowed.isEmpty()) {
            return List.of();
        }
        userDeviceRepository.updateBlocked(allowed, blocked, at);
        return userDeviceRepository.findByGuestUuidIn(allowed).stream()
                .map(this::toDeviceDto)
                .toList();
    }

    private static String adminId(String adminSub) {
        return adminSub;
    }

    private AdminUserDto toUserDto(User user) {
        long deviceCount = user.getDevices() != null ? user.getDevices().size() : 0;
        long photoCount = user.getPhotos() != null ? user.getPhotos().size() : 0;
        return new AdminUserDto(
                user.getId(),
                user.getCognitoId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                deviceCount,
                photoCount
        );
    }

    private AdminDeviceDto toDeviceDto(UserDevice device) {
        long photoCount = device.getPhotos() != null ? device.getPhotos().size() : 0;
        return new AdminDeviceDto(
                device.getGuestUuid(),
                device.getUser() != null ? device.getUser().getId() : null,
                device.getUser() != null ? device.getUser().getCognitoId() : null,
                device.getLastActive(),
                device.getBlocked(),
                device.getBlockedAt(),
                photoCount
        );
    }
}
