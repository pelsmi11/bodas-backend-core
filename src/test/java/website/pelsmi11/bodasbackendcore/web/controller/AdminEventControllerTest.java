package website.pelsmi11.bodasbackendcore.web.controller;

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

/**
 * Security matrix + ownership routing for admin events (spec 001 US4/T026):
 * guest → 100% 403; admin → 200; ownership rejections surface as 404.
 */
@WebMvcTest(controllers = AdminEventController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, TestSecurityConfig.class, JwtSubjectExtractor.class})
class AdminEventControllerTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String ADMIN = "Bearer test-jwt";
    private static final String GUEST = "Bearer test-guest";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminEventService adminEventService;
    @MockitoBean
    private AdminPhotoService adminPhotoService;

    private EventResponse sampleResponse() {
        return new EventResponse(1, "Boda Test", "ev123456", TestDataFactory.TEST_ADMIN_SUB,
                java.time.OffsetDateTime.now(), "desc", EventStatus.ACTIVE, true,
                java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(), 5L);
    }

    @Test
    void create_adminJwt_returns200Envelope() throws Exception {
        when(adminEventService.create(any(), eq(TestDataFactory.TEST_ADMIN_SUB)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/admin/events")
                        .header(AUTH_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Boda Test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void adminOperations_guest_rejected100percent() throws Exception {
        String body = "{\"name\":\"x\"}";
        mockMvc.perform(get("/api/v1/admin/events").header(AUTH_HEADER, GUEST)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/events").header(AUTH_HEADER, GUEST)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/events/1").header(AUTH_HEADER, GUEST)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/admin/events/1").header(AUTH_HEADER, GUEST)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/admin/events/1").header(AUTH_HEADER, GUEST)).andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoints_withoutJwt_return401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/admin/events/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void findAll_scopedBySub() throws Exception {
        when(adminEventService.findAll(eq(TestDataFactory.TEST_ADMIN_SUB), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/events").header(AUTH_HEADER, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void findById_foreignEvent_returns404() throws Exception {
        when(adminEventService.findById(TestDataFactory.TEST_ADMIN_SUB, 2))
                .thenThrow(CustomErrorException.handlerCustomError("Event not found", org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/events/{id}", 2).header(AUTH_HEADER, ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_ownedEvent_returns200() throws Exception {
        when(adminEventService.update(eq(TestDataFactory.TEST_ADMIN_SUB), eq(1), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/admin/events/{id}", 1)
                        .header(AUTH_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void regenerateToken_ownedEvent_returnsToken() throws Exception {
        when(adminEventService.regenerateToken(TestDataFactory.TEST_ADMIN_SUB, 1))
                .thenReturn(new EventTokenRegenerateResponse("newtoken1"));

        mockMvc.perform(post("/api/v1/admin/events/{id}/regenerate-token", 1).header(AUTH_HEADER, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("newtoken1"));
    }

    @Test
    void delete_ownedEvent_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/events/{id}", 1).header(AUTH_HEADER, ADMIN))
                .andExpect(status().isNoContent());
    }

    @Test
    void stats_ownedEvent_returnsEnvelope() throws Exception {
        when(adminEventService.getStats(TestDataFactory.TEST_ADMIN_SUB, 1))
                .thenReturn(new EventPhotoStatsDto(5L, 2L, 2L, 1L, 0L));

        mockMvc.perform(get("/api/v1/admin/events/{id}/stats", 1).header(AUTH_HEADER, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(5));
    }

    @Test
    void eventPhotos_ownedEvent_returnsEnvelope() throws Exception {
        AdminPhotoDto photoDto = new AdminPhotoDto(TestDataFactory.TEST_PHOTO_ID, "k", ModerationStatus.PENDING,
                null, null, TestDataFactory.TEST_GUEST_UUID, "ev123456", 1,
                java.time.OffsetDateTime.now(), null, null, null, null);
        when(adminPhotoService.findByEvent(eq(TestDataFactory.TEST_ADMIN_SUB), eq(1), eq(ModerationStatus.PENDING),
                eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(photoDto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/events/{id}/photos", 1)
                        .header(AUTH_HEADER, ADMIN)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].eventId").value(1));
    }
}
