package website.pelsmi11.bodasbackendcore.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoFeedDto;
import website.pelsmi11.bodasbackendcore.domain.service.feed.EventFeedService;
import website.pelsmi11.bodasbackendcore.domain.service.feed.EventFeedStreamService;
import website.pelsmi11.bodasbackendcore.web.config.SecurityConfig;
import website.pelsmi11.bodasbackendcore.web.exception.GlobalExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.http.MediaType;

@WebMvcTest(controllers = EventFeedController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, EventFeedControllerTest.NoopJwtDecoderConfig.class})
class EventFeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventFeedService eventFeedService;
    @MockitoBean
    private EventFeedStreamService eventFeedStreamService;

    @TestConfiguration
    static class NoopJwtDecoderConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> TestDataFactory.jwt();
        }
    }

    @Test
    void getInitialFeed_returnsApprovedPhotos() throws Exception {
        PhotoFeedDto dto = new PhotoFeedDto("photo-1", "uploads/key.jpg", "Test User", "2026-01-01T00:00:00Z");
        when(eventFeedService.getApprovedFeed("ev123456")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/events/ev123456/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].photoId").value("photo-1"))
                .andExpect(jsonPath("$.data[0].s3Key").value("uploads/key.jpg"));
    }

    @Test
    void getInitialFeed_emptyList_returns200() throws Exception {
        when(eventFeedService.getApprovedFeed("ev123456")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/events/ev123456/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void streamLivePhotos_returnsEventStream() throws Exception {
        when(eventFeedStreamService.subscribe("ev123456")).thenReturn(new SseEmitter(30000L));

        mockMvc.perform(get("/api/v1/events/ev123456/stream"))
                .andExpect(status().isOk());
    }
}
