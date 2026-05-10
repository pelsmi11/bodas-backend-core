package website.pelsmi11.bodasbackendcore.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PhotoConfirmRequest {

    @NotBlank
    private String eventToken;

    @NotNull
    private UUID guestId;

    @NotBlank
    private String s3Key;
}
