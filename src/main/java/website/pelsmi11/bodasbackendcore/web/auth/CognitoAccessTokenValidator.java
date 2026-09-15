package website.pelsmi11.bodasbackendcore.web.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import website.pelsmi11.bodasbackendcore.web.config.CognitoProperties;

/**
 * Validates Cognito Access Token claims beyond signature/expiry/issuer:
 * {@code token_use == "access"} and {@code client_id == app.cognito.audience}.
 * ID tokens and tokens from other app clients are rejected.
 */
public class CognitoAccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    static final OAuth2Error INVALID_TOKEN_USE =
            new OAuth2Error("invalid_token", "token_use must be access", null);
    static final OAuth2Error INVALID_CLIENT =
            new OAuth2Error("invalid_token", "client_id does not match the expected app client", null);

    private final CognitoProperties properties;

    public CognitoAccessTokenValidator(CognitoProperties properties) {
        this.properties = properties;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String tokenUse = jwt.getClaimAsString("token_use");
        if (!"access".equals(tokenUse)) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN_USE);
        }
        String clientId = jwt.getClaimAsString("client_id");
        if (properties.getAudience() == null || !properties.getAudience().equals(clientId)) {
            return OAuth2TokenValidatorResult.failure(INVALID_CLIENT);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
