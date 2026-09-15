package website.pelsmi11.bodasbackendcore.web.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import website.pelsmi11.bodasbackendcore.web.auth.CognitoAccessTokenValidator;

/**
 * Builds the Cognito-aware {@link JwtDecoder}: signature + expiry via JWKS,
 * issuer, and access-token claim validation (token_use/client_id) via
 * {@link CognitoAccessTokenValidator}. Externally configured, no secrets.
 */
@Configuration
public class CognitoJwtDecoderConfig {

    @Bean
    public JwtDecoder cognitoJwtDecoder(CognitoProperties properties) {
        // JWK-set URI construction is offline-safe (no issuer discovery call at
        // startup); JWKS is fetched lazily on the first token validation.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                properties.getIssuerUri() + "/.well-known/jwks.json").build();
        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(properties.getIssuerUri());
        OAuth2TokenValidator<Jwt> accessTokenValidator =
                new CognitoAccessTokenValidator(properties);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, accessTokenValidator));
        return decoder;
    }
}
