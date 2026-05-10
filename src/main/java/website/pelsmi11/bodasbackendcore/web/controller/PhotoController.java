package website.pelsmi11.bodasbackendcore.web.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import website.pelsmi11.bodasbackendcore.domain.dto.ApiResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoConfirmRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.UploadUrlRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.UploadUrlResponse;
import website.pelsmi11.bodasbackendcore.domain.service.PhotoService;

@RestController
@RequestMapping("/api/v1/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    /**
     * Returns a signed URL plus S3 key for direct guest upload to S3.
     * Uses optional Cognito JWT to bind the request to the authenticated identity.
     */
    @PostMapping("/presigned-url")
    public ApiResponse<UploadUrlResponse> getPresignedUrl(
            @Valid @RequestBody UploadUrlRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UploadUrlResponse uploadUrl = photoService.generateUploadUrl(
                request.getEventToken(),
                request.getGuestId(),
                request.getFileName(),
                request.getContentType(),
                getCognitoId(jwt)
        );

        return ApiResponse.ok(uploadUrl);
    }

    /**
     * Confirms the upload and creates a moderation-pending photo record.
     */
    @PostMapping("/confirm")
    public ApiResponse<String> confirmUpload(
            @Valid @RequestBody PhotoConfirmRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        photoService.registerPendingPhoto(request, getCognitoId(jwt));
        return ApiResponse.ok("Foto registrada exitosamente. Pendiente de moderacion.");
    }

    private String getCognitoId(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("sub") : null;
    }
}
