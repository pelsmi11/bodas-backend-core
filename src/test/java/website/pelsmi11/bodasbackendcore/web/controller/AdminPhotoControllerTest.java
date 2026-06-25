package website.pelsmi11.bodasbackendcore.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.TestSecurityConfig;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminPhotoDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.PhotoModerationRequest;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.PhotoStatusUpdateRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.admin.AdminPhotoService;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.web.auth.JwtSubjectExtractor;
import website.pelsmi11.bodasbackendcore.web.config.SecurityConfig;
import website.pelsmi11.bodasbackendcore.web.exception.GlobalExceptionHandler;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminPhotoController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, TestSecurityConfig.class, JwtSubjectExtractor.class})
class AdminPhotoControllerTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER = "Bearer test-jwt";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminPhotoService adminPhotoService;

    private AdminPhotoDto sampleDto() {
        return new AdminPhotoDto(
                TestDataFactory.TEST_PHOTO_ID, "uploads/key.jpg", ModerationStatus.PENDING,
                "user", "cognito", TestDataFactory.TEST_GUEST_UUID, "ev123456", 1,
                OffsetDateTime.now(), null, null, null, null);
    }

    @Test
    void findPending_returns200() throws Exception {
        when(adminPhotoService.findPendingGlobal(any()))
                .thenReturn(new PageImpl<>(List.of(sampleDto()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/photos/pending")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].s3Key").value("uploads/key.jpg"));
    }

    @Test
    void findById_existing_returns200() throws Exception {
        when(adminPhotoService.findById(TestDataFactory.TEST_PHOTO_ID)).thenReturn(sampleDto());

        mockMvc.perform(get("/api/v1/admin/photos/" + TestDataFactory.TEST_PHOTO_ID)
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TestDataFactory.TEST_PHOTO_ID.toString()));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(adminPhotoService.findById(any())).thenThrow(
                new CustomErrorException("Photo not found", org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/photos/" + UUID.randomUUID())
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_validBody_returns200() throws Exception {
        PhotoStatusUpdateRequest request = new PhotoStatusUpdateRequest();
        request.setStatus(ModerationStatus.APPROVED);
        AdminPhotoDto approved = sampleDto();
        when(adminPhotoService.updateStatus(any(), any(), eq(TestDataFactory.TEST_ADMIN_SUB))).thenReturn(approved);

        mockMvc.perform(patch("/api/v1/admin/photos/" + TestDataFactory.TEST_PHOTO_ID + "/status")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_missingStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/photos/" + TestDataFactory.TEST_PHOTO_ID + "/status")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moderate_validBody_returns200() throws Exception {
        PhotoModerationRequest request = new PhotoModerationRequest();
        request.setPhotoIds(List.of(TestDataFactory.TEST_PHOTO_ID));
        request.setAction(PhotoModerationRequest.ModerationAction.APPROVE);
        when(adminPhotoService.moderate(any(), eq(TestDataFactory.TEST_ADMIN_SUB)))
                .thenReturn(List.of(sampleDto()));

        mockMvc.perform(post("/api/v1/admin/photos/moderate")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].s3Key").value("uploads/key.jpg"));
    }

    @Test
    void moderate_emptyPhotoIds_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/photos/moderate")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoIds\":[],\"action\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void softDelete_validIds_returns200() throws Exception {
        when(adminPhotoService.softDelete(any())).thenReturn(2);

        mockMvc.perform(delete("/api/v1/admin/photos")
                        .header(AUTH_HEADER, BEARER)
                        .param("ids", TestDataFactory.TEST_PHOTO_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    void softDelete_invalidUuid_returns400() throws Exception {
        when(adminPhotoService.softDelete("not-a-uuid")).thenThrow(
                new CustomErrorException("Invalid UUID format in 'ids'", org.springframework.http.HttpStatus.BAD_REQUEST));

        mockMvc.perform(delete("/api/v1/admin/photos")
                        .header(AUTH_HEADER, BEARER)
                        .param("ids", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void softDelete_missingIdsParam_returns400() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/photos")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isBadRequest());
    }
}
