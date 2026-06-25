package website.pelsmi11.bodasbackendcore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import website.pelsmi11.bodasbackendcore.TestDataFactory;

/**
 * Replaces the Cognito JwtDecoder with a stub that accepts any token and returns
 * a Jwt with a fixed subject. Used by @WebMvcTest admin controller tests to
 * exercise the real JWT security filter without contacting AWS Cognito.
 */
@Configuration
public class TestSecurityConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .claim("sub", TestDataFactory.TEST_ADMIN_SUB)
                .issuer("https://example.com/test-issuer")
                .build();
    }
}
