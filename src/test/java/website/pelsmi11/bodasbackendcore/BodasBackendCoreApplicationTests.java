package website.pelsmi11.bodasbackendcore;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;
import website.pelsmi11.bodasbackendcore.persistence.repository.EventRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserDeviceRepository;
import website.pelsmi11.bodasbackendcore.persistence.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "COGNITO_ISSUER_URI=https://example.com/test-issuer",
        "AWS_REGION=us-east-1",
        "AWS_PROFILE=test",
        "S3_BUCKET_NAME=test-bucket",
        "REDIS_HOST=127.0.0.1",
        "app.redis.listener.enabled=false",
        "WEBHOOK_SECRET=test-webhook-secret",
        "MODERATION_SECRET_NAME=test-secret"
})
class BodasBackendCoreApplicationTests {

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

    @Test
    void contextLoads() {
    }

}
