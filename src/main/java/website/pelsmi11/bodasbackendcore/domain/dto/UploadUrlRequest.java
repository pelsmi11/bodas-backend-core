package website.pelsmi11.bodasbackendcore.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadUrlRequest {

    @NotBlank
    private String eventToken;

    @NotBlank
    private String guestId;

    @NotBlank
    private String fileName;

    @NotBlank
    private String contentType;
}
