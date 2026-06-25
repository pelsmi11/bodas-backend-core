package website.pelsmi11.bodasbackendcore.web.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;

/**
 * Extracts the Cognito subject ({@code sub} claim) from a JWT, throwing 401
 * when the principal is missing. Shared by admin controllers to avoid duplication.
 */
@Component
public class JwtSubjectExtractor {

    /**
     * Returns the {@code sub} claim from the given JWT.
     *
     * @throws CustomErrorException with 401 status when the JWT is absent.
     */
    public String requireSub(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw CustomErrorException.handlerCustomError("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        return jwt.getClaimAsString("sub");
    }
}
