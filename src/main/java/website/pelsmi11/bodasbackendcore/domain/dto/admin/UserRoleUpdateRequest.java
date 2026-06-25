package website.pelsmi11.bodasbackendcore.domain.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;

@Data
public class UserRoleUpdateRequest {

    @NotNull
    private UserRole role;
}
