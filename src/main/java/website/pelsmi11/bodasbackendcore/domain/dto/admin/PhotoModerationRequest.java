package website.pelsmi11.bodasbackendcore.domain.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PhotoModerationRequest {

    @NotEmpty
    private List<UUID> photoIds;

    private ModerationAction action;

    private String reason;

    public enum ModerationAction {
        APPROVE,
        REJECT
    }
}
