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

    /** Scoped to events administered by the caller (no global pending queue). */
    @GetMapping("/pending")
    public ApiResponse<Page<AdminPhotoDto>> findPending(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "uploadedAt,desc") Pageable pageable
    ) {
        return ApiResponse.ok(adminPhotoService.findPending(jwtSubjectExtractor.requireSub(jwt), pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminPhotoDto> findById(@AuthenticationPrincipal Jwt jwt, @PathVariable java.util.UUID id) {
        return ApiResponse.ok(adminPhotoService.findById(jwtSubjectExtractor.requireSub(jwt), id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminPhotoDto> updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable java.util.UUID id,
            @Valid @RequestBody PhotoStatusUpdateRequest request
    ) {
        return ApiResponse.ok(adminPhotoService.updateStatus(jwtSubjectExtractor.requireSub(jwt), id, request));
    }

    @PostMapping("/moderate")
    public ApiResponse<List<AdminPhotoDto>> moderate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PhotoModerationRequest request
    ) {
        return ApiResponse.ok(adminPhotoService.moderate(jwtSubjectExtractor.requireSub(jwt), request));
    }

    @DeleteMapping
    public ApiResponse<Integer> softDelete(@AuthenticationPrincipal Jwt jwt, @RequestParam("ids") String ids) {
        return ApiResponse.ok(adminPhotoService.softDelete(jwtSubjectExtractor.requireSub(jwt), ids));
    }
}
