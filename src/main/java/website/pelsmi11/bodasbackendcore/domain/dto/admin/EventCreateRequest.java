package website.pelsmi11.bodasbackendcore.domain.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class EventCreateRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String adminId;

    private OffsetDateTime eventDate;

    @Size(max = 1000)
    private String description;
}
