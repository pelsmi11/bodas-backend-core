package website.pelsmi11.bodasbackendcore.domain.service.feed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedEvent;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoFeedMapperTest {

    @Mock private PhotoReadUrlService photoReadUrlService;
    @InjectMocks private PhotoFeedMapper mapper;

    @Test
    void toInternalEvent_keepsS3KeyInsideRedisPayload() {
        Photo photo = TestDataFactory.photo();

        PhotoFeedEvent event = mapper.toInternalEvent(photo);

        assertThat(event.photoId()).isEqualTo(photo.getId().toString());
        assertThat(event.s3Key()).isEqualTo(photo.getS3Key());
        assertThat(event.uploadedAt()).isEqualTo(photo.getUploadedAt().toString());
    }

    @Test
    void toPublicDto_replacesS3KeyWithSignedUrl() {
        PhotoFeedEvent event = new PhotoFeedEvent("photo-id", "uploads/key.jpg", "2026-01-01T00:00:00Z");
        when(photoReadUrlService.createReadUrl("uploads/key.jpg")).thenReturn("https://s3.example.com/signed");

        PhotoFeedDto dto = mapper.toPublicDto(event);

        assertThat(dto.photoId()).isEqualTo("photo-id");
        assertThat(dto.imageUrl()).isEqualTo("https://s3.example.com/signed");
        assertThat(dto.uploadedAt()).isEqualTo("2026-01-01T00:00:00Z");
    }
}
