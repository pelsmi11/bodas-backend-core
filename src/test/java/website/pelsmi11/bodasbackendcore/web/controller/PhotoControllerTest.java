package website.pelsmi11.bodasbackendcore.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.dto.PhotoConfirmRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.UploadUrlResponse;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.PhotoService;
import website.pelsmi11.bodasbackendcore.web.config.SecurityConfig;
import website.pelsmi11.bodasbackendcore.web.exception.GlobalExceptionHandler;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PhotoController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, PhotoControllerTest.NoopJwtDecoderConfig.class})
class PhotoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PhotoService photoService;

    @TestConfiguration
    static class NoopJwtDecoderConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> TestDataFactory.jwt();
        }
    }

    @Test
    void getPresignedUrl_validBody_returns200() throws Exception {
        when(photoService.generateUploadUrl(eq("ev123456"), any(), eq("photo.jpg"), eq("image/jpeg"), any()))
                .thenReturn(new UploadUrlResponse("https://s3.example.com/signed", "uploads/key.jpg"));

        mockMvc.perform(post("/api/v1/photos/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventToken\":\"ev123456\",\"guestId\":\"11111111-1111-1111-1111-111111111111\",\"fileName\":\"photo.jpg\",\"contentType\":\"image/jpeg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadUrl").value("https://s3.example.com/signed"))
                .andExpect(jsonPath("$.data.s3Key").value("uploads/key.jpg"));
    }

    @Test
    void getPresignedUrl_missingEventToken_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/photos/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestId\":\"11111111-1111-1111-1111-111111111111\",\"fileName\":\"photo.jpg\",\"contentType\":\"image/jpeg\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getPresignedUrl_invalidContentType_returns400() throws Exception {
        when(photoService.generateUploadUrl(any(), any(), any(), eq("video/mp4"), any()))
                .thenThrow(new CustomErrorException("Unsupported image format"));

        mockMvc.perform(post("/api/v1/photos/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventToken\":\"ev123456\",\"guestId\":\"11111111-1111-1111-1111-111111111111\",\"fileName\":\"photo.jpg\",\"contentType\":\"video/mp4\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported image format"));
    }

    @Test
    void confirmUpload_validBody_returns200() throws Exception {
        PhotoConfirmRequest request = new PhotoConfirmRequest();
        request.setEventToken("ev123456");
        request.setGuestId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        request.setS3Key("uploads/ev123456/guest/photo.jpg");

        mockMvc.perform(post("/api/v1/photos/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void confirmUpload_missingGuestId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/photos/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventToken\":\"ev123456\",\"s3Key\":\"uploads/key.jpg\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void confirmUpload_serviceThrows404_returns404() throws Exception {
        doThrow(new CustomErrorException("Event not found or inactive", org.springframework.http.HttpStatus.NOT_FOUND))
                .when(photoService).registerPendingPhoto(any(), any());

        mockMvc.perform(post("/api/v1/photos/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventToken\":\"nonexistent\",\"guestId\":\"11111111-1111-1111-1111-111111111111\",\"s3Key\":\"uploads/key.jpg\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found or inactive"));
    }
}
