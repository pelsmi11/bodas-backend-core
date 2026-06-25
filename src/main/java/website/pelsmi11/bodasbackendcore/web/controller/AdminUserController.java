package website.pelsmi11.bodasbackendcore.web.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import website.pelsmi11.bodasbackendcore.domain.dto.ApiResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminDeviceDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminUserDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.DeviceBlockRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.UserRoleUpdateRequest;
import website.pelsmi11.bodasbackendcore.domain.service.admin.AdminUserService;
import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Users & Devices", description = "User management, role assignment, device block/unblock")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public ApiResponse<Page<AdminUserDto>> findAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable
    ) {
        return ApiResponse.ok(adminUserService.findAllUsers(search, role, pageable));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<AdminUserDto> findUserById(@PathVariable Integer id) {
        return ApiResponse.ok(adminUserService.findUserById(id));
    }

    @GetMapping("/users/{id}/devices")
    public ApiResponse<Page<AdminDeviceDto>> findUserDevices(
            @PathVariable Integer id,
            @PageableDefault(size = 20, sort = "lastActive,desc") Pageable pageable
    ) {
        return ApiResponse.ok(adminUserService.findDevicesByUser(id, pageable));
    }

    @PatchMapping("/users/{id}/role")
    public ApiResponse<AdminUserDto> setRole(
            @PathVariable Integer id,
            @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        return ApiResponse.ok(adminUserService.setRole(id, request));
    }

    @PostMapping("/devices/block")
    public ApiResponse<List<AdminDeviceDto>> blockDevices(@Valid @RequestBody DeviceBlockRequest request) {
        if (request.getBlocked() == null) request.setBlocked(true);
        return ApiResponse.ok(adminUserService.blockDevices(request));
    }

    @PostMapping("/devices/unblock")
    public ApiResponse<List<AdminDeviceDto>> unblockDevices(@Valid @RequestBody DeviceBlockRequest request) {
        request.setBlocked(false);
        return ApiResponse.ok(adminUserService.blockDevices(request));
    }
}
