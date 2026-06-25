package website.pelsmi11.bodasbackendcore.web.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminPhotoDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.PhotoModerationRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.PhotoStatusUpdateRequest;
import website.pelsmi11.bodasbackendcore.domain.service.admin.AdminPhotoService;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.web.auth.JwtSubjectExtractor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/photos")
@Tag(name = "Admin Photos", description = "Photo moderation queue, status changes, batch ops, soft-delete")
public class AdminPhotoController {

    private final AdminPhotoService adminPhotoService;
    private final JwtSubjectExtractor jwtSubjectExtractor;

    public AdminPhotoController(AdminPhotoService adminPhotoService, JwtSubjectExtractor jwtSubjectExtractor) {
        this.adminPhotoService = adminPhotoService;
        this.jwtSubjectExtractor = jwtSubjectExtractor;
    }

    @GetMapping("/pending")
    public ApiResponse<Page<AdminPhotoDto>> findPending(
            @PageableDefault(size = 20, sort = "uploadedAt,desc") Pageable pageable
    ) {
        return ApiResponse.ok(adminPhotoService.findPendingGlobal(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminPhotoDto> findById(@PathVariable java.util.UUID id) {
        return ApiResponse.ok(adminPhotoService.findById(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminPhotoDto> updateStatus(
            @PathVariable java.util.UUID id,
            @Valid @RequestBody PhotoStatusUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.ok(adminPhotoService.updateStatus(id, request, jwtSubjectExtractor.requireSub(jwt)));
    }

    @PostMapping("/moderate")
    public ApiResponse<List<AdminPhotoDto>> moderate(
            @Valid @RequestBody PhotoModerationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.ok(adminPhotoService.moderate(request, jwtSubjectExtractor.requireSub(jwt)));
    }

    @DeleteMapping
    public ApiResponse<Integer> softDelete(@RequestParam("ids") String ids) {
        return ApiResponse.ok(adminPhotoService.softDelete(ids));
    }
}
