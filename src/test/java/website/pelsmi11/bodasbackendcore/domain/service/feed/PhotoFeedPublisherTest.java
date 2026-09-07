package website.pelsmi11.bodasbackendcore.domain.service.feed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedEvent;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoFeedPublisherTest {

    @Mock
    private PhotoFeedMapper photoFeedMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PhotoFeedPublisher photoFeedPublisher;

    @Test
    void publishApprovedPhoto_happyPath_publishesToRedisChannel() throws Exception {
        Photo photo = TestDataFactory.photo();
        PhotoFeedEvent dto = new PhotoFeedEvent("photo-id", "key", "2026-01-01T00:00:00Z");
        when(photoFeedMapper.toInternalEvent(photo)).thenReturn(dto);
        when(objectMapper.writeValueAsString(dto)).thenReturn("{\"photoId\":\"photo-id\"}");

        photoFeedPublisher.publishApprovedPhoto(photo);

        verify(redisTemplate).convertAndSend("event-feed:" + photo.getEvent().getToken(), "{\"photoId\":\"photo-id\"}");
    }

    @Test
    void publishApprovedPhoto_serializationFails_throws500() throws Exception {
        Photo photo = TestDataFactory.photo();
        PhotoFeedEvent dto = new PhotoFeedEvent("photo-id", "key", "2026-01-01T00:00:00Z");
        when(photoFeedMapper.toInternalEvent(photo)).thenReturn(dto);
        when(objectMapper.writeValueAsString(dto)).thenThrow(new JsonProcessingException("boom") {
        });

        assertThatThrownBy(() -> photoFeedPublisher.publishApprovedPhoto(photo))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("serialize");

        verify(redisTemplate, never()).convertAndSend(any(), any());
    }
}
