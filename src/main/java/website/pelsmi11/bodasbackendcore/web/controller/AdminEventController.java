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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import website.pelsmi11.bodasbackendcore.domain.dto.ApiResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminPhotoDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventCreateRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventPhotoStatsDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventTokenRegenerateResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventUpdateRequest;
import website.pelsmi11.bodasbackendcore.domain.service.admin.AdminEventService;
import website.pelsmi11.bodasbackendcore.domain.service.admin.AdminPhotoService;
import website.pelsmi11.bodasbackendcore.persistence.model.EventStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.web.auth.JwtSubjectExtractor;

@RestController
@RequestMapping("/api/v1/admin/events")
@Tag(name = "Admin Events", description = "Event lifecycle management (CRUD, token regen, stats)")
public class AdminEventController {

    private final AdminEventService adminEventService;
    private final AdminPhotoService adminPhotoService;
    private final JwtSubjectExtractor jwtSubjectExtractor;

    public AdminEventController(
            AdminEventService adminEventService,
            AdminPhotoService adminPhotoService,
            JwtSubjectExtractor jwtSubjectExtractor
    ) {
        this.adminEventService = adminEventService;
        this.adminPhotoService = adminPhotoService;
        this.jwtSubjectExtractor = jwtSubjectExtractor;
    }

    @PostMapping
    public ApiResponse<EventResponse> create(
            @Valid @RequestBody EventCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        EventResponse response = adminEventService.create(request, jwtSubjectExtractor.requireSub(jwt));
        return ApiResponse.ok(response);
    }

    @GetMapping
    public ApiResponse<Page<EventResponse>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EventStatus status,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable
    ) {
        return ApiResponse.ok(adminEventService.findAll(search, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<EventResponse> findById(@PathVariable Integer id) {
        return ApiResponse.ok(adminEventService.findById(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<EventResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody EventUpdateRequest request
    ) {
        return ApiResponse.ok(adminEventService.update(id, request));
    }

    @PostMapping("/{id}/regenerate-token")
    public ApiResponse<EventTokenRegenerateResponse> regenerateToken(@PathVariable Integer id) {
        return ApiResponse.ok(adminEventService.regenerateToken(id));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<EventResponse> activate(@PathVariable Integer id) {
        return ApiResponse.ok(adminEventService.setActive(id, true));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<EventResponse> deactivate(@PathVariable Integer id) {
        return ApiResponse.ok(adminEventService.setActive(id, false));
    }

    @GetMapping("/{id}/stats")
    public ApiResponse<EventPhotoStatsDto> getStats(@PathVariable Integer id) {
        return ApiResponse.ok(adminEventService.getStats(id));
    }

    @GetMapping("/{id}/photos")
    public ApiResponse<Page<AdminPhotoDto>> findEventPhotos(
            @PathVariable Integer id,
            @RequestParam(required = false) ModerationStatus status,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @PageableDefault(size = 20, sort = "uploadedAt,desc") Pageable pageable
    ) {
        return ApiResponse.ok(adminPhotoService.findByEvent(id, status, includeDeleted, pageable));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        adminEventService.delete(id);
    }
}
