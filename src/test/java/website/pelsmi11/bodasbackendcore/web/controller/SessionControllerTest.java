package website.pelsmi11.bodasbackendcore.web.controller;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import website.pelsmi11.bodasbackendcore.TestSecurityConfig;
import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionEventDto;
import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionReconciliationDto;
import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionResponse;
import website.pelsmi11.bodasbackendcore.domain.dto.session.SessionUserDto;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.session.CognitoGetUserService;
import website.pelsmi11.bodasbackendcore.domain.service.session.SessionBootstrapService;
import website.pelsmi11.bodasbackendcore.web.auth.JwtSubjectExtractor;
import website.pelsmi11.bodasbackendcore.web.config.SecurityConfig;
import website.pelsmi11.bodasbackendcore.web.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Envelope + contract test for the single idempotent session endpoint.
 * Also carries the FR-012a negative assertion at the HTTP envelope level.
 */
@WebMvcTest(controllers = SessionController.class)
@Import({GlobalExceptionHandler.class, website.pelsmi11.bodasbackendcore.web.config.SecurityConfig.class, TestSecurityConfig.class, JwtSubjectExtractor.class})
class SessionControllerTest {

    private static final String ADMIN = "Bearer test-jwt";
    private static final String GUEST = "Bearer test-guest";
    private static final UUID GUEST_UUID = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CognitoGetUserService cognitoGetUserService;
    @MockitoBean
    private SessionBootstrapService sessionBootstrapService;

    private SessionResponse session(String role) {
        SessionResponse response = new SessionResponse();
        response.setUser(new SessionUserDto(42, "guest@example.com"));
        response.setRole(role);
        response.setEvents(List.of(new SessionEventDto(7, "Boda", "ev123", "PARTICIPANT")));
        response.setReconciliation(new SessionReconciliationDto("NO_CHANGE", false));
        return response;
    }

    @Test
    void bootstrap_admin_returnsEnvelopeWithoutPhotoFields() throws Exception {
        when(cognitoGetUserService.fetchEmail("test-jwt")).thenReturn("admin@example.com");
        when(sessionBootstrapService.reconcile(eq("test-admin-sub"), eq(true), eq("admin@example.com"),
                eq(GUEST_UUID), eq(null))).thenReturn(session("ADMIN"));

        String body = mockMvc.perform(post("/api/v1/auth/session/bootstrap")
                        .header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestUuid\":\"" + GUEST_UUID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.user.hasName").value(false))
                .andExpect(jsonPath("$.data.reconciliation.rotateGuestUuid").value(false))
                .andReturn().getResponse().getContentAsString();

        // FR-012a: no photo ids/urls/s3Key/gallery collections anywhere
        String lower = body.toLowerCase();
        org.assertj.core.api.Assertions.assertThat(lower).doesNotContain("photo", "s3key", "imageurl", "gallery");
    }

    @Test
    void bootstrap_guest_returnsRoleGuest() throws Exception {
        when(cognitoGetUserService.fetchEmail("test-guest")).thenReturn(null);
        when(sessionBootstrapService.reconcile(eq("test-guest-sub"), eq(false), org.mockito.ArgumentMatchers.isNull(),
                eq(GUEST_UUID), eq("ev123"))).thenReturn(session("GUEST"));

        mockMvc.perform(post("/api/v1/auth/session/bootstrap")
                        .header("Authorization", GUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestUuid\":\"" + GUEST_UUID + "\",\"eventToken\":\"ev123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("GUEST"));
    }

    @Test
    void bootstrap_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/session/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestUuid\":\"" + GUEST_UUID + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bootstrap_getUserThrottled_returns503Neutral() throws Exception {
        when(cognitoGetUserService.fetchEmail("test-jwt")).thenThrow(
                CustomErrorException.handlerCustomError(
                        "Temporary issue contacting the identity provider; please retry",
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));

        mockMvc.perform(post("/api/v1/auth/session/bootstrap")
                        .header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestUuid\":\"" + GUEST_UUID + "\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Temporary issue contacting the identity provider; please retry"));
    }

    @Test
    void bootstrap_invalidGuestUuid_returns400() throws Exception {
        when(cognitoGetUserService.fetchEmail("test-jwt")).thenReturn(null);
        mockMvc.perform(post("/api/v1/auth/session/bootstrap")
                        .header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
