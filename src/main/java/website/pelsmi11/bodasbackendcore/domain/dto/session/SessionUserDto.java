package website.pelsmi11.bodasbackendcore.domain.dto.session;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Normalized session profile. email is string|null (Cognito GetUser source).
 * hasName is always false in this feature (guest names are never stored).
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionUserDto {

    private Integer id;
    private String email;
    private boolean hasName;

    public SessionUserDto(Integer id, String email) {
        this.id = id;
        this.email = email;
        this.hasName = false;
    }
}
