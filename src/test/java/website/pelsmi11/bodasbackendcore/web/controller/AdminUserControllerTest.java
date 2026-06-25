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
import website.pelsmi11.bodasbackendcore.domain.dto.admin.UserRoleUpdateRequest;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.domain.service.admin.AdminUserService;
import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;
import website.pelsmi11.bodasbackendcore.web.config.SecurityConfig;
import website.pelsmi11.bodasbackendcore.web.exception.GlobalExceptionHandler;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, TestSecurityConfig.class})
class AdminUserControllerTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER = "Bearer test-jwt";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminUserService adminUserService;

    private AdminUserDto sampleUserDto() {
        return new AdminUserDto(1, "cognito-1", "Test", "test@example.com",
                UserRole.GUEST, OffsetDateTime.now(), null, 2, 10);
    }

    @Test
    void findAllUsers_returns200() throws Exception {
        when(adminUserService.findAllUsers(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleUserDto()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void findUserById_existing_returns200() throws Exception {
        when(adminUserService.findUserById(1)).thenReturn(sampleUserDto());

        mockMvc.perform(get("/api/v1/admin/users/1")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void findUserById_notFound_returns404() throws Exception {
        when(adminUserService.findUserById(99)).thenThrow(
                new CustomErrorException("User not found", org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/users/99")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isNotFound());
    }

    @Test
    void findUserDevices_returns200() throws Exception {
        AdminDeviceDto deviceDto = new AdminDeviceDto(
                TestDataFactory.TEST_GUEST_UUID, 1, "cognito-1",
                OffsetDateTime.now(), false, null, 5);
        when(adminUserService.findDevicesByUser(eq(1), any()))
                .thenReturn(new PageImpl<>(List.of(deviceDto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/users/1/devices")
                        .header(AUTH_HEADER, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].photoCount").value(5));
    }

    @Test
    void setRole_validBody_returns200() throws Exception {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setRole(UserRole.ADMIN);
        AdminUserDto adminDto = sampleUserDto();
        when(adminUserService.setRole(eq(1), any())).thenReturn(adminDto);

        mockMvc.perform(patch("/api/v1/admin/users/1/role")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void setRole_missingRole_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/1/role")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blockDevices_validBody_returns200() throws Exception {
        DeviceBlockRequest request = new DeviceBlockRequest();
        request.setGuestUuids(List.of(TestDataFactory.TEST_GUEST_UUID));
        request.setBlocked(true);
        AdminDeviceDto blockedDto = new AdminDeviceDto(
                TestDataFactory.TEST_GUEST_UUID, 1, "cognito-1",
                OffsetDateTime.now(), true, OffsetDateTime.now(), 5);
        when(adminUserService.blockDevices(any())).thenReturn(List.of(blockedDto));

        mockMvc.perform(post("/api/v1/admin/devices/block")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].blocked").value(true));
    }

    @Test
    void blockDevices_emptyUuids_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/devices/block")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestUuids\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unblockDevices_validBody_returns200() throws Exception {
        DeviceBlockRequest request = new DeviceBlockRequest();
        request.setGuestUuids(List.of(TestDataFactory.TEST_GUEST_UUID));
        AdminDeviceDto unblocked = new AdminDeviceDto(
                TestDataFactory.TEST_GUEST_UUID, 1, "cognito-1",
                OffsetDateTime.now(), false, null, 5);
        when(adminUserService.blockDevices(any())).thenReturn(List.of(unblocked));

        mockMvc.perform(post("/api/v1/admin/devices/unblock")
                        .header(AUTH_HEADER, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].blocked").value(false));
    }
}
