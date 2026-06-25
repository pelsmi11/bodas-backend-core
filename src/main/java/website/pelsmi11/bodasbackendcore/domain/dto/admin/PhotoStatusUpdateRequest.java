package website.pelsmi11.bodasbackendcore.domain.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;

@Data
public class PhotoStatusUpdateRequest {

    @NotNull
    private ModerationStatus status;

    private String reason;
}
