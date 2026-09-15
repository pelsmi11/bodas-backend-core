package website.pelsmi11.bodasbackendcore.web.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import website.pelsmi11.bodasbackendcore.domain.service.admin.AdminUserService;
import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Users & Devices", description = "User management (participants of administered events) and device block/unblock")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final website.pelsmi11.bodasbackendcore.web.auth.JwtSubjectExtractor jwtSubjectExtractor;

    public AdminUserController(
            AdminUserService adminUserService,
            website.pelsmi11.bodasbackendcore.web.auth.JwtSubjectExtractor jwtSubjectExtractor
    ) {
        this.adminUserService = adminUserService;
        this.jwtSubjectExtractor = jwtSubjectExtractor;
    }

    @GetMapping("/users")
    public ApiResponse<Page<AdminUserDto>> findAllUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable
    ) {
        return ApiResponse.ok(adminUserService.findAllUsers(jwtSubjectExtractor.requireSub(jwt), search, role, pageable));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<AdminUserDto> findUserById(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return ApiResponse.ok(adminUserService.findUserById(jwtSubjectExtractor.requireSub(jwt), id));
    }

    @GetMapping("/users/{id}/devices")
    public ApiResponse<Page<AdminDeviceDto>> findUserDevices(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @PageableDefault(size = 20, sort = "lastActive,desc") Pageable pageable
    ) {
        return ApiResponse.ok(adminUserService.findDevicesByUser(jwtSubjectExtractor.requireSub(jwt), id, pageable));
    }

    // PATCH /users/{id}/role RETIRED (spec 001): users.role is an informational
    // mirror of cognito:groups, written ONLY by the trusted bootstrap.

    @PostMapping("/devices/block")
    public ApiResponse<List<AdminDeviceDto>> blockDevices(
            @Valid @RequestBody DeviceBlockRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (request.getBlocked() == null) request.setBlocked(true);
        return ApiResponse.ok(adminUserService.blockDevices(jwtSubjectExtractor.requireSub(jwt), request));
    }

    @PostMapping("/devices/unblock")
    public ApiResponse<List<AdminDeviceDto>> unblockDevices(
            @Valid @RequestBody DeviceBlockRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        request.setBlocked(false);
        return ApiResponse.ok(adminUserService.blockDevices(jwtSubjectExtractor.requireSub(jwt), request));
    }
}
