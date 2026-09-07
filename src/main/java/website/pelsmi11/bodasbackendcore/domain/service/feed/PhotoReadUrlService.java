package website.pelsmi11.bodasbackendcore.domain.service.feed;

import io.awspring.cloud.s3.S3Template;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Creates short-lived read URLs for objects in the private photos bucket.
 */
@Component
public class PhotoReadUrlService {

    private final S3Template s3Template;
    private final String bucketName;
    private final Duration downloadUrlTtl;

    public PhotoReadUrlService(
            S3Template s3Template,
            @Value("${app.s3.bucket-name}") String bucketName,
            @Value("${app.s3.download-url-ttl:PT1H}") Duration downloadUrlTtl
    ) {
        this.s3Template = s3Template;
        this.bucketName = bucketName;
        this.downloadUrlTtl = downloadUrlTtl;
    }

    public String createReadUrl(String s3Key) {
        return s3Template.createSignedGetURL(bucketName, s3Key, downloadUrlTtl).toString();
    }
}
