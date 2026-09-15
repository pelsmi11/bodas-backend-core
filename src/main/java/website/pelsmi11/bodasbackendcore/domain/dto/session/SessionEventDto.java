package website.pelsmi11.bodasbackendcore.domain.dto.session;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Event available to the authenticated user (participation or administered). */
@Data
public class SessionEventDto {

    private Integer eventId;
    private String name;
    private String eventToken;
    private String role;

    public SessionEventDto(Integer eventId, String name, String eventToken, String role) {
        this.eventId = eventId;
        this.name = name;
        this.eventToken = eventToken;
        this.role = role;
    }
}
