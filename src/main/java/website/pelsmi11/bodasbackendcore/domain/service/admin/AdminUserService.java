package website.pelsmi11.bodasbackendcore.domain.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Administrative operations for users and device management.
 */
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PhotoRepository photoRepository;

    public AdminUserService(
            UserRepository userRepository,
            UserDeviceRepository userDeviceRepository,
            PhotoRepository photoRepository
    ) {
        this.userRepository = userRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.photoRepository = photoRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> findAllUsers(String search, UserRole role, Pageable pageable) {
        Page<User> page;
        if (role != null && search != null && !search.isBlank()) {
            page = userRepository.findByRoleAndSearch(role, search, pageable);
        } else if (role != null) {
            page = userRepository.findByRoleOrderByCreatedAtDesc(role, pageable);
        } else if (search != null && !search.isBlank()) {
            page = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc(search, search, pageable);
        } else {
            page = userRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::toUserDto);
    }

    @Transactional(readOnly = true)
    public AdminUserDto findUserById(Integer id) {
        return toUserDto(requireUser(id));
    }

    @Transactional(readOnly = true)
    public Page<AdminDeviceDto> findDevicesByUser(Integer userId, Pageable pageable) {
        requireUser(userId);
        return userDeviceRepository.findByUserIdOrderByLastActiveDesc(userId, pageable)
                .map(this::toDeviceDto);
    }

    @Transactional
    public List<AdminDeviceDto> blockDevices(DeviceBlockRequest request) {
        boolean blocked = request.getBlocked() == null || request.getBlocked();
        OffsetDateTime at = blocked ? OffsetDateTime.now() : null;
        userDeviceRepository.updateBlocked(request.getGuestUuids(), blocked, at);
        return userDeviceRepository.findByGuestUuidIn(request.getGuestUuids()).stream()
                .map(this::toDeviceDto)
                .toList();
    }

    @Transactional
    public AdminUserDto setRole(Integer userId, UserRoleUpdateRequest request) {
        User user = requireUser(userId);
        user.setRole(request.getRole());
        User saved = userRepository.save(user);
        return toUserDto(saved);
    }

    private User requireUser(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> CustomErrorException.handlerCustomError("User not found", HttpStatus.NOT_FOUND));
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
