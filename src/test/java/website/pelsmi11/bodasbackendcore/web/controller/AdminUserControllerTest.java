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
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminDeviceDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.AdminUserDto;
import website.pelsmi11.bodasbackendcore.domain.dto.admin.DeviceBlockRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.admin.AdminUserService;
import website.pelsmi11.bodasbackendcore.web.auth.JwtSubjectExtractor;
import website.pelsmi11.bodasbackendcore.web.config.SecurityConfig;
import website.pelsmi11.bodasbackendcore.web.exception.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, TestSecurityConfig.class, JwtSubjectExtractor.class})
class AdminUserControllerTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String ADMIN = "Bearer test-jwt";
    private static final String GUEST = "Bearer test-guest";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private AdminUserService adminUserService;

    private AdminUserDto adminDto() {
        return new AdminUserDto(1, "cognito-1", "Test User", "test@example.com",
                website.pelsmi11.bodasbackendcore.persistence.model.UserRole.GUEST,
                java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(), 1L, 2L);
    }

    @Test
    void findAllUsers_admin_returns200Envelope() throws Exception {
        org.mockito.Mockito.when(adminUserService.findAllUsers(
                        eq(TestDataFactory.TEST_ADMIN_SUB), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(adminDto())));

        mockMvc.perform(get("/api/v1/admin/users").header(AUTH_HEADER, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void findAllUsers_guest_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header(AUTH_HEADER, GUEST))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAllUsers_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchRole_isRetired_returns404or405() throws Exception {
        // users.role is a bootstrap mirror; the endpoint must be gone
        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", 1)
                        .header(AUTH_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void blockDevices_admin_scopesToOwnedEvents() throws Exception {
        org.mockito.Mockito.when(adminUserService.blockDevices(eq(TestDataFactory.TEST_ADMIN_SUB), any()))
                .thenReturn(List.of(new AdminDeviceDto(TestDataFactory.TEST_GUEST_UUID, 10, "cognito-10",
                        java.time.OffsetDateTime.now(), true, java.time.OffsetDateTime.now(), 0L)));

        mockMvc.perform(post("/api/v1/admin/devices/block")
                        .header(AUTH_HEADER, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestUuids\":[\"" + TestDataFactory.TEST_GUEST_UUID + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void errorResponse_usesRealApiErrorShape() throws Exception {
        org.mockito.Mockito.when(adminUserService.findUserById(eq(TestDataFactory.TEST_ADMIN_SUB), eq(404)))
                .thenThrow(CustomErrorException.handlerCustomError("User not found", org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/users/{id}", 404).header(AUTH_HEADER, ADMIN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
