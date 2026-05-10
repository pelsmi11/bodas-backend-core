package website.pelsmi11.bodasbackendcore.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadUrlResponse {

    private String uploadUrl;

    private String s3Key;
}
