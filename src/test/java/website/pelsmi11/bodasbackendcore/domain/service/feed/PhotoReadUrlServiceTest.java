package website.pelsmi11.bodasbackendcore.domain.service.feed;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhotoReadUrlServiceTest {

    @Test
    void createReadUrl_usesConfiguredBucketAndTtl() throws MalformedURLException {
        S3Template s3Template = mock(S3Template.class);
        when(s3Template.createSignedGetURL("photos", "uploads/key.jpg", Duration.ofHours(1)))
                .thenReturn(new URL("https://s3.example.com/signed"));
        PhotoReadUrlService service = new PhotoReadUrlService(s3Template, "photos", Duration.ofHours(1));

        assertThat(service.createReadUrl("uploads/key.jpg")).isEqualTo("https://s3.example.com/signed");
        verify(s3Template).createSignedGetURL("photos", "uploads/key.jpg", Duration.ofHours(1));
    }
}
