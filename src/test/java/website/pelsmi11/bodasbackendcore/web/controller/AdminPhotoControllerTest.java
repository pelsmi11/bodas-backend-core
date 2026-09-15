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

/**
 * Ownership + security matrix for admin photos (spec 001 T026):
 * guest → 403; admin → 200; foreign/missing photo → 404; envelope is real.
 */
@WebMvcTest(controllers = AdminPhotoController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, TestSecurityConfig.class, JwtSubjectExtractor.class})
class AdminPhotoControllerTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String ADMIN = "Bearer test-jwt";
    private static final String GUEST = "Bearer test-guest";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminPhotoService adminPhotoService;

    private AdminPhotoDto sampleDto() {
        return new AdminPhotoDto(
                TestDataFactory.TEST_PHOTO_ID,
                "uploads/ev1/k.jpg",
                ModerationStatus.PENDING,
                null, null,
                TestDataFactory.TEST_GUEST_UUID,
                "ev123456",
                1,
                OffsetDateTime.now(), null, null, null, null);
    }

    @Test
    void findPending_guest_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/photos/pending").header(AUTH_HEADER, GUEST))
                .andExpect(status().isForbidden());
    }

    @Test
    void findPending_admin_scopedBySub() throws Exception {
        when(adminPhotoService.findPending(eq(TestDataFactory.TEST_ADMIN_SUB), any()))
                .thenReturn(new PageImpl<>(List.of(sampleDto()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/photos/pending").header(AUTH_HEADER, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].s3Key").value("uploads/ev1/k.jpg"));
    }

    @Test
    void findById_owned_returns200() throws Exception {
        when(adminPhotoService.findById(TestDataFactory.TEST_ADMIN_SUB, TestDataFactory.TEST_PHOTO_ID))
                .thenReturn(sampleDto());

        mockMvc.perform(get("/api/v1/admin/photos/{id}", TestDataFactory.TEST_PHOTO_ID).header(AUTH_HEADER, ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void findById_foreignOrMissing_returns404() throws Exception {
        when(adminPhotoService.findById(eq(TestDataFactory.TEST_ADMIN_SUB), any()))
                .thenThrow(CustomErrorException.handlerCustomError("Photo not found", org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/photos/{id}", TestDataFactory.TEST_PHOTO_ID).header(AUTH_HEADER, ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_owned_returns200() throws Exception {
        AdminPhotoDto approved = new AdminPhotoDto(TestDataFactory.TEST_PHOTO_ID, "k",
                ModerationStatus.APPROVED, null, null, TestDataFactory.TEST_GUEST_UUID, "ev1", 1,
                OffsetDateTime.now(), OffsetDateTime.now(), "sub", null, null);
        when(adminPhotoService.updateStatus(eq(TestDataFactory.TEST_ADMIN_SUB), any(), any())).thenReturn(approved);

        mockMvc.perform(patch("/api/v1/admin/photos/{id}/status", TestDataFactory.TEST_PHOTO_ID)
                        .header(AUTH_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void moderate_owned_returns200() throws Exception {
        when(adminPhotoService.moderate(eq(TestDataFactory.TEST_ADMIN_SUB), any()))
                .thenReturn(List.of(sampleDto()));

        mockMvc.perform(post("/api/v1/admin/photos/moderate")
                        .header(AUTH_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoIds\":[\"" + TestDataFactory.TEST_PHOTO_ID + "\"],\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].s3Key").value("uploads/ev1/k.jpg"));
    }

    @Test
    void softDelete_owned_returnsCount() throws Exception {
        when(adminPhotoService.softDelete(eq(TestDataFactory.TEST_ADMIN_SUB), any())).thenReturn(2);

        mockMvc.perform(delete("/api/v1/admin/photos")
                        .header(AUTH_HEADER, ADMIN)
                        .param("ids", TestDataFactory.TEST_PHOTO_ID + "," + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    void softDelete_invalidUuid_returns400() throws Exception {
        when(adminPhotoService.softDelete(eq(TestDataFactory.TEST_ADMIN_SUB), any()))
                .thenThrow(CustomErrorException.handlerCustomError("Invalid UUID format in 'ids'",
                        org.springframework.http.HttpStatus.BAD_REQUEST));

        mockMvc.perform(delete("/api/v1/admin/photos")
                        .header(AUTH_HEADER, ADMIN)
                        .param("ids", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
