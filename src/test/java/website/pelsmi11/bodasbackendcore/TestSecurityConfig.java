package website.pelsmi11.bodasbackendcore;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import website.pelsmi11.bodasbackendcore.web.config.CognitoProperties;

/**
 * Replaces the Cognito JwtDecoder with a stub that accepts configured tokens and
 * returns a Jwt with deterministic claims. Used by @WebMvcTest controller tests
 * to exercise the real security filter (hasRole, converter) without contacting
 * AWS Cognito. Token-keyed semantics:
 * - "test-jwt"  -> admin sub, groups [ADMIN]
 * - "test-guest" -> guest sub, groups [GUEST]
 * - anything else -> 401 via JwtException
 */
@TestConfiguration
public class TestSecurityConfig {

    public static final String ISSUER = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_TEST";
    public static final String CLIENT_ID = "test-app-client-id";

    @Bean
    CognitoProperties cognitoProperties() {
        CognitoProperties properties = new CognitoProperties();
        properties.setIssuerUri(ISSUER);
        properties.setAudience(CLIENT_ID);
        return properties;
    }

    @Bean
    @org.springframework.context.annotation.Primary
    JwtDecoder testJwtDecoder() {
        return token -> {
            if (!"test-jwt".equals(token) && !"test-guest".equals(token)) {
                throw new JwtException("Unrecognized test token");
            }
            boolean admin = "test-jwt".equals(token);
            return Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .claim("sub", admin ? TestDataFactory.TEST_ADMIN_SUB : "test-guest-sub")
                    .claim("cognito:groups", admin ? List.of("ADMIN") : List.of("GUEST"))
                    .claim("token_use", "access")
                    .claim("client_id", CLIENT_ID)
                    .issuer(ISSUER)
                    .build();
        };
    }
}
