package website.pelsmi11.bodasbackendcore;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import website.pelsmi11.bodasbackendcore.persistence.repository.EventRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserDeviceRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "COGNITO_ISSUER_URI=https://example.com/test-issuer",
        "COGNITO_APP_CLIENT_ID=test-client",
        "AWS_REGION=us-east-1",
        "AWS_PROFILE=test",
        "S3_BUCKET_NAME=test-bucket",
        "REDIS_HOST=127.0.0.1",
        "app.redis.listener.enabled=false",
        "WEBHOOK_SECRET=test-webhook-secret"
})
@AutoConfigureMockMvc
class BodasBackendCoreApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AwsCredentialsProvider awsCredentialsProvider;

    @MockitoBean
    private S3Template s3Template;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private PhotoRepository photoRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserDeviceRepository userDeviceRepository;

    @MockitoBean
    private website.pelsmi11.bodasbackendcore.persistence.repository.EventParticipantRepository eventParticipantRepository;

    // T006-fix: the REAL session services must be registered by component
    // scanning; only their repository/client dependencies are mocked above and
    // below (CognitoIdentityProviderClient stays mocked — offline-safe).

    @org.springframework.beans.factory.annotation.Autowired
    private website.pelsmi11.bodasbackendcore.domain.service.session.CognitoGetUserService cognitoGetUserService;

    @org.springframework.beans.factory.annotation.Autowired
    private website.pelsmi11.bodasbackendcore.domain.service.session.SessionBootstrapService sessionBootstrapService;

    @MockitoBean
    private software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient cognitoIdentityProviderClient;

    @Test
    void contextLoads() {
    }

    @Test
    void sessionServicesAreRegisteredByComponentScanning() {
        org.assertj.core.api.Assertions.assertThat(cognitoGetUserService).isNotNull();
        org.assertj.core.api.Assertions.assertThat(sessionBootstrapService).isNotNull();
    }

    @Test
    void livenessProbe_isPublic_andUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void otherActuatorEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    // On Fargate there is no ~/.aws: only the default chain reaches the ECS task role.
    @Test
    void awsCredentials_useDefaultChain_notAPinnedProfile() {
        org.assertj.core.api.Assertions.assertThat(awsCredentialsProvider)
                .isInstanceOf(DefaultCredentialsProvider.class);
    }
}
