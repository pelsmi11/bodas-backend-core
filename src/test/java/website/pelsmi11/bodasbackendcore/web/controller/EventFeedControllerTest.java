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
import website.pelsmi11.bodasbackendcore.domain.dto.PublicEventDto;
import website.pelsmi11.bodasbackendcore.domain.service.feed.EventFeedService;
import website.pelsmi11.bodasbackendcore.domain.service.feed.EventFeedStreamService;
import website.pelsmi11.bodasbackendcore.domain.service.photo.EventResolver;
import website.pelsmi11.bodasbackendcore.web.config.SecurityConfig;
import website.pelsmi11.bodasbackendcore.web.exception.GlobalExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.time.OffsetDateTime;

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
    @MockitoBean
    private EventResolver eventResolver;

    @TestConfiguration
    static class NoopJwtDecoderConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> TestDataFactory.jwt();
        }
    }

    @Test
    void getInitialFeed_returnsApprovedPhotos() throws Exception {
        PhotoFeedDto dto = new PhotoFeedDto("photo-1", "https://s3.example.com/signed", "2026-01-01T00:00:00Z");
        when(eventFeedService.getApprovedFeed("ev123456")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/events/ev123456/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].photoId").value("photo-1"))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://s3.example.com/signed"))
                .andExpect(jsonPath("$.data[0].s3Key").doesNotExist());
    }

    @Test
    void getEventDetails_returnsPublicFieldsOnly() throws Exception {
        PublicEventDto event = new PublicEventDto(
                "ev123456",
                "Boda Test",
                OffsetDateTime.parse("2026-01-01T18:00:00Z"),
                "Jardin"
        );
        when(eventFeedService.getEventDetails("ev123456")).thenReturn(event);

        mockMvc.perform(get("/api/v1/events/ev123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventToken").value("ev123456"))
                .andExpect(jsonPath("$.data.name").value("Boda Test"))
                .andExpect(jsonPath("$.data.adminId").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist());
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
        when(eventResolver.findActiveEvent("ev123456")).thenReturn(TestDataFactory.event());
        when(eventFeedStreamService.subscribe("ev123456")).thenReturn(new SseEmitter(30000L));

        mockMvc.perform(get("/api/v1/events/ev123456/stream"))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflight_allowedOrigin_returnsCorsHeaders() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/v1/events/ev123456")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsPreflight_unknownOrigin_isRejected() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/v1/events/ev123456")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
