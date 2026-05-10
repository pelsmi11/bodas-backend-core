package website.pelsmi11.bodasbackendcore.domain.service.photo;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Creates deterministic S3 key layouts for guest photo uploads.
 */
@Component
public class PhotoKeyFactory {

    /**
     * Builds an S3 key using event token, guest UUID and original file name.
     */
    public String buildUploadKey(String eventToken, UUID guestUuid, String fileName) {
        String safeFileName = sanitizeFileName(fileName);
        String uniqueFileName = addShortIdToFileName(safeFileName);
        return String.format("uploads/%s/%s/%s", eventToken, guestUuid, uniqueFileName);
    }

    /**
     * Ensures an uploaded key belongs to the expected event and guest prefix.
     */
    public boolean belongsToGuestAndEvent(String s3Key, String eventToken, UUID guestUuid) {
        String expectedPrefix = String.format("uploads/%s/%s/", eventToken, guestUuid);
        return s3Key.startsWith(expectedPrefix);
    }

    private String sanitizeFileName(String fileName) {
        String normalizedFileName = fileName.replace("\\", "/");
        return normalizedFileName.substring(normalizedFileName.lastIndexOf("/") + 1);
    }

    private String addShortIdToFileName(String fileName) {
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        int extensionSeparatorIndex = fileName.lastIndexOf(".");

        if (extensionSeparatorIndex <= 0 || extensionSeparatorIndex == fileName.length() - 1) {
            return String.format("%s_%s", fileName, shortId);
        }

        String name = fileName.substring(0, extensionSeparatorIndex);
        String extension = fileName.substring(extensionSeparatorIndex);
        return String.format("%s_%s%s", name, shortId, extension);
    }
}
