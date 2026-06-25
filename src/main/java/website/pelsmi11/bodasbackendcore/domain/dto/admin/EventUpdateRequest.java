package website.pelsmi11.bodasbackendcore.domain.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class EventUpdateRequest {

    @Size(max = 255)
    private String name;

    private OffsetDateTime eventDate;

    @Size(max = 1000)
    private String description;

    private String status;
}
