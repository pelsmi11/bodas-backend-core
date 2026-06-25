package website.pelsmi11.bodasbackendcore.domain.service.feed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.repository.PhotoRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventFeedServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private EventFeedService eventFeedService;

    @Test
    void getApprovedFeed_returnsMappedDtos() {
        Photo photo = TestDataFactory.photo();
        when(photoRepository.findByEventTokenAndStatusOrderByUploadedAtDesc("ev123456", ModerationStatus.APPROVED))
                .thenReturn(List.of(photo));

        List<PhotoFeedDto> result = eventFeedService.getApprovedFeed("ev123456");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).photoId()).isEqualTo(photo.getId().toString());
        assertThat(result.get(0).s3Key()).isEqualTo(photo.getS3Key());
        assertThat(result.get(0).userName()).isEqualTo(photo.getUser().getName());
    }

    @Test
    void getApprovedFeed_emptyList_returnsEmpty() {
        when(photoRepository.findByEventTokenAndStatusOrderByUploadedAtDesc("ev123456", ModerationStatus.APPROVED))
                .thenReturn(List.of());

        List<PhotoFeedDto> result = eventFeedService.getApprovedFeed("ev123456");

        assertThat(result).isEmpty();
    }

    @Test
    void toDto_withNullUser_returnsNullUserName() {
        Photo photo = TestDataFactory.photo();
        photo.setUser(null);

        PhotoFeedDto dto = eventFeedService.toDto(photo);

        assertThat(dto.userName()).isNull();
    }

    @Test
    void toDto_withNullUploadedAt_returnsNullTimestamp() {
        Photo photo = TestDataFactory.photo();
        photo.setUploadedAt(null);

        PhotoFeedDto dto = eventFeedService.toDto(photo);

        assertThat(dto.uploadedAt()).isNull();
    }
}
