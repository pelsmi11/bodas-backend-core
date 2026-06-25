package website.pelsmi11.bodasbackendcore.domain.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DeviceBlockRequest {

    @NotEmpty
    private List<UUID> guestUuids;

    private Boolean blocked;
}
