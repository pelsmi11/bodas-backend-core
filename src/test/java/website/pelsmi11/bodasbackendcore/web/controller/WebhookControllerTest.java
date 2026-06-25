package website.pelsmi11.bodasbackendcore.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.ModerationWebhookRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.ModerationService;
import website.pelsmi11.bodasbackendcore.web.config.SecurityConfig;
import website.pelsmi11.bodasbackendcore.web.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WebhookController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, WebhookControllerTest.NoopJwtDecoderConfig.class})
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WebhookController webhookController;

    @MockitoBean
    private ModerationService moderationService;

    @TestConfiguration
    static class NoopJwtDecoderConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> TestDataFactory.jwt();
        }
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookController, "expectedSecret", "test-webhook-secret");
    }

    @Test
    void handleModerationResult_validSecret_returns200() throws Exception {
        ModerationWebhookRequest request = new ModerationWebhookRequest("uploads/key.jpg", true, "ok");

        mockMvc.perform(post("/api/v1/webhooks/moderation")
                        .header("X-Webhook-Secret", "test-webhook-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(moderationService).processModerationResult(any());
    }

    @Test
    void handleModerationResult_missingSecretHeader_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/moderation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"s3Key\":\"uploads/key.jpg\",\"isApproved\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void handleModerationResult_wrongSecret_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/moderation")
                        .header("X-Webhook-Secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"s3Key\":\"uploads/key.jpg\",\"isApproved\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void handleModerationResult_missingS3Key_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/moderation")
                        .header("X-Webhook-Secret", "test-webhook-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isApproved\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleModerationResult_missingIsApproved_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/moderation")
                        .header("X-Webhook-Secret", "test-webhook-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"s3Key\":\"uploads/key.jpg\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleModerationResult_photoNotFound_returns404() throws Exception {
        doThrow(new CustomErrorException("Photo not found", org.springframework.http.HttpStatus.NOT_FOUND))
                .when(moderationService).processModerationResult(any());

        mockMvc.perform(post("/api/v1/webhooks/moderation")
                        .header("X-Webhook-Secret", "test-webhook-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"s3Key\":\"missing.jpg\",\"isApproved\":true}"))
                .andExpect(status().isNotFound());
    }
}
