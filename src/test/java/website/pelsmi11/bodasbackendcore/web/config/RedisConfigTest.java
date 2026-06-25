package website.pelsmi11.bodasbackendcore.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.domain.service.feed.EventFeedStreamService;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisConfigTest {

    @Mock
    private EventFeedStreamService eventFeedStreamService;
    @Mock
    private ObjectMapper objectMapper;

    private RedisConfig redisConfig;
    private MessageListener feedMessageListener;

    @BeforeEach
    void setUp() {
        redisConfig = new RedisConfig();
        feedMessageListener = redisConfig.feedMessageListener(eventFeedStreamService, objectMapper);
    }

    @Test
    void onMessage_validChannelAndPayload_dispatchesToStreamService() throws Exception {
        Message message = mock(Message.class);
        when(message.getChannel()).thenReturn("event-feed:ev123456".getBytes(StandardCharsets.UTF_8));
        when(message.getBody()).thenReturn("{\"photoId\":\"id\"}".getBytes(StandardCharsets.UTF_8));
        PhotoFeedDto dto = new PhotoFeedDto("id", "key", "user", "2026-01-01T00:00:00Z");
        when(objectMapper.readValue(any(String.class), eq(PhotoFeedDto.class))).thenReturn(dto);

        feedMessageListener.onMessage(message, null);

        verify(eventFeedStreamService).dispatchNewPhoto("ev123456", dto);
    }

    @Test
    void onMessage_malformedPayload_doesNotThrowAndDoesNotDispatch() throws Exception {
        Message message = mock(Message.class);
        when(message.getChannel()).thenReturn("event-feed:ev123456".getBytes(StandardCharsets.UTF_8));
        when(message.getBody()).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));
        when(objectMapper.readValue(any(String.class), eq(PhotoFeedDto.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("bad") {
                });

        feedMessageListener.onMessage(message, null);

        verify(eventFeedStreamService, never()).dispatchNewPhoto(any(), any());
    }

    @Test
    void onMessage_channelWithoutPrefix_dispatchesWithEmptyToken() throws Exception {
        Message message = mock(Message.class);
        when(message.getChannel()).thenReturn("unknown-channel".getBytes(StandardCharsets.UTF_8));
        when(message.getBody()).thenReturn("{\"photoId\":\"id\"}".getBytes(StandardCharsets.UTF_8));
        PhotoFeedDto dto = new PhotoFeedDto("id", "key", "user", "2026-01-01T00:00:00Z");
        when(objectMapper.readValue(any(String.class), eq(PhotoFeedDto.class))).thenReturn(dto);

        feedMessageListener.onMessage(message, null);

        verify(eventFeedStreamService).dispatchNewPhoto(eq(""), eq(dto));
    }

    @Test
    void onMessage_nullBody_doesNotThrow() {
        Message message = mock(Message.class);
        when(message.getChannel()).thenReturn("event-feed:ev123456".getBytes(StandardCharsets.UTF_8));
        when(message.getBody()).thenReturn(null);

        feedMessageListener.onMessage(message, null);

        verify(eventFeedStreamService, never()).dispatchNewPhoto(any(), any());
    }

    @Test
    void extractEventToken_validChannel_returnsToken() {
        assertThat(redisConfig.extractEventToken("event-feed:ev123456")).isEqualTo("ev123456");
    }

    @Test
    void extractEventToken_channelWithoutColon_returnsEmpty() {
        assertThat(redisConfig.extractEventToken("nocolon")).isEqualTo("");
    }

    @Test
    void extractEventToken_channelWithMultipleColons_returnsEverythingAfterFirst() {
        assertThat(redisConfig.extractEventToken("event-feed:ev:123:456")).isEqualTo("ev:123:456");
    }
}
