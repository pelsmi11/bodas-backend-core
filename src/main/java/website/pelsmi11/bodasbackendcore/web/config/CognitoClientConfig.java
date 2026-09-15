package website.pelsmi11.bodasbackendcore.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * Provides the AWS SDK v2 {@link CognitoIdentityProviderClient} used exclusively
 * for the Cognito GetUser call (email attribution). Credentials come from the
 * default provider chain (SSO profile in this lab); no secrets in code.
 */
@Configuration
public class CognitoClientConfig {

    /**
     * Bound from {@code app.cognito.*} (environment-backed). Marked @Primary so
     * test configurations may provide additional stubs without ambiguity.
     */
    @Bean
    @ConfigurationProperties("app.cognito")
    @Primary
    public CognitoProperties cognitoProperties() {
        return new CognitoProperties();
    }

    @Bean
    public CognitoIdentityProviderClient cognitoIdentityProviderClient(CognitoProperties properties) {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(properties.getRegion() == null || properties.getRegion().isBlank()
                        ? "us-east-1"
                        : properties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
