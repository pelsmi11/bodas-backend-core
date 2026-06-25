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
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventPhotoStatsDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.EventTokenRegenerateResponse;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.admin.AdminEventService;
import website.pelsmi11.bodasbackendcore.domain.service.admin.AdminPhotoService;
import website.pelsmi11.bodasbackendcore.persistence.model.EventStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.web.auth.JwtSubjectExtractor;
import website.pelsmi11.bodasbackendcore.web.config.SecurityConfig;
import website.pelsmi11.bodasbackendcore.web.exception.GlobalExceptionHandler;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminEventController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, TestSecurityConfig.class, JwtSubjectExtractor.class})
class AdminEventControllerTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER = "Bearer test-jwt";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminEventService adminEventService;
    @MockitoBean
    private AdminPhotoService adminPhotoService;

    private EventResponse sampleResponse() {
        return new EventResponse(1, "Boda Test", "ev123456", "admin-sub",
                OffsetDateTime.now(), "desc", EventStatus.ACTIVE, true,
                OffsetDateTime.now(), OffsetDateTime.now(), 5L);
    }

    @Test
    void create_withJwt_returns200() throws Exception {
        when(adminEventService.create(any(), eq(TestDataFactory.TEST_ADMIN_SUB)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/admin/events")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Boda Test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.token").value("ev123456"));
    }

    @Test
    void create_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Boda Test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/events")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_withParams_returns200() throws Exception {
        when(adminEventService.findAll(eq("boda"), eq(EventStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/events")
                        .header(AUTH_HEADER, BEARER)
                        .param("search", "boda")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void findById_existing_returns200() throws Exception {
        when(adminEventService.findById(1)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/admin/events/1")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(adminEventService.findById(99)).thenThrow(
                new CustomErrorException("Event not found", org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/events/99")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_validBody_returns200() throws Exception {
        when(adminEventService.update(eq(1), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/admin/events/1")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void regenerateToken_returns200() throws Exception {
        when(adminEventService.regenerateToken(1)).thenReturn(new EventTokenRegenerateResponse("newtok99"));

        mockMvc.perform(post("/api/v1/admin/events/1/regenerate-token")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("newtok99"));
    }

    @Test
    void activate_returns200() throws Exception {
        when(adminEventService.setActive(1, true)).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/admin/events/1/activate")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk());
    }

    @Test
    void deactivate_returns200() throws Exception {
        when(adminEventService.setActive(1, false)).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/admin/events/1/deactivate")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk());
    }

    @Test
    void getStats_returns200() throws Exception {
        when(adminEventService.getStats(1)).thenReturn(new EventPhotoStatsDto(100, 10, 80, 5, 5));

        mockMvc.perform(get("/api/v1/admin/events/1/stats")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(100));
    }

    @Test
    void findEventPhotos_returns200() throws Exception {
        AdminPhotoDto photoDto = new AdminPhotoDto(
                TestDataFactory.TEST_PHOTO_ID, "uploads/key.jpg", ModerationStatus.PENDING,
                "user", "cognito", TestDataFactory.TEST_GUEST_UUID, "ev123456", 1,
                OffsetDateTime.now(), null, null, null, null);
        when(adminPhotoService.findByEvent(eq(1), any(), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(photoDto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/events/1/photos")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].s3Key").value("uploads/key.jpg"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/events/1")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isNoContent());
    }
}
