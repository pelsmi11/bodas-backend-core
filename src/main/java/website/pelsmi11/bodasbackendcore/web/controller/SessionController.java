package website.pelsmi11.bodasbackendcore.web.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import website.pelsmi11.bodasbackendcore.domain.dto.ApiResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionBootstrapRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionResponse;
import website.pelsmi11.bodasbackendcore.domain.service.session.CognitoGetUserService;
import website.pelsmi11.bodasbackendcore.domain.service.session.SessionBootstrapService;
import website.pelsmi11.bodasbackendcore.web.auth.CognitoGroupsAuthoritiesConverter;

/**
 * Single idempotent session endpoint (login AND restoration):
 * POST /api/v1/auth/session/bootstrap.
 *
 * The Cognito GetUser email lookup happens BEFORE the reconciliation transaction
 * opens; email is string|null. The response contains NO photos (FR-012a).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Session", description = "Idempotent session bootstrap and reconciliation")
public class SessionController {

    private final CognitoGetUserService cognitoGetUserService;
    private final SessionBootstrapService sessionBootstrapService;

    public SessionController(
            CognitoGetUserService cognitoGetUserService,
            SessionBootstrapService sessionBootstrapService) {
        this.cognitoGetUserService = cognitoGetUserService;
        this.sessionBootstrapService = sessionBootstrapService;
    }

    @PostMapping("/session/bootstrap")
    public ApiResponse<SessionResponse> bootstrap(
            @AuthenticationPrincipal Jwt jwt,
            @jakarta.validation.Valid @RequestBody SessionBootstrapRequest request) {
        if (jwt == null) {
            throw website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException
                    .handlerCustomError("Authentication required", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        // 1) GetUser BEFORE the reconciliation transaction opens
        String email = cognitoGetUserService.fetchEmail(jwt.getTokenValue());
        // 2) reconcile inside the transaction (guestUuid kept public name)
        SessionResponse response = sessionBootstrapService.reconcile(
                jwt.getClaimAsString("sub"),
                hasAdmin(jwt),
                email,
                request.getGuestUuid(),
                request.getEventToken());
        return ApiResponse.ok(response);
    }

    private boolean hasAdmin(Jwt jwt) {
        java.util.List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        return groups != null && groups.contains(CognitoGroupsAuthoritiesConverter.ADMIN_GROUP);
    }
}
