package website.pelsmi11.bodasbackendcore.domain.service.photo;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoKeyFactoryTest {

    private static final UUID GUEST_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EVENT_TOKEN = "ev123456";

    private final PhotoKeyFactory photoKeyFactory = new PhotoKeyFactory();

    @Test
    void buildUploadKey_withExtension_returnsKeyWithShortIdBeforeExtension() {
        String key = photoKeyFactory.buildUploadKey(EVENT_TOKEN, GUEST_UUID, "photo.jpg");

        assertThat(key).startsWith("uploads/" + EVENT_TOKEN + "/" + GUEST_UUID + "/photo_");
        assertThat(key).endsWith(".jpg");
        assertThat(key).hasSize(("uploads/" + EVENT_TOKEN + "/" + GUEST_UUID + "/photo_").length() + 8 + ".jpg".length());
    }

    @Test
    void buildUploadKey_withoutExtension_appendsShortIdAtEnd() {
        String key = photoKeyFactory.buildUploadKey(EVENT_TOKEN, GUEST_UUID, "filename");

        assertThat(key).startsWith("uploads/" + EVENT_TOKEN + "/" + GUEST_UUID + "/filename_");
        assertThat(key).doesNotContain(".");
    }

    @Test
    void buildUploadKey_withFullPath_stripsDirectoryAndKeepsBasename() {
        String key = photoKeyFactory.buildUploadKey(EVENT_TOKEN, GUEST_UUID, "C:\\Users\\guest\\photo.png");

        assertThat(key).startsWith("uploads/" + EVENT_TOKEN + "/" + GUEST_UUID + "/photo_");
        assertThat(key).endsWith(".png");
        assertThat(key).doesNotContain("Users");
        assertThat(key).doesNotContain("guest\\");
    }

    @Test
    void buildUploadKey_withUnixPath_stripsDirectoryAndKeepsBasename() {
        String key = photoKeyFactory.buildUploadKey(EVENT_TOKEN, GUEST_UUID, "/tmp/uploads/img.webp");

        assertThat(key).startsWith("uploads/" + EVENT_TOKEN + "/" + GUEST_UUID + "/img_");
        assertThat(key).endsWith(".webp");
        assertThat(key).doesNotContain("/tmp/");
    }

    @Test
    void buildUploadKey_generatesUniqueKeysForSameInput() {
        String key1 = photoKeyFactory.buildUploadKey(EVENT_TOKEN, GUEST_UUID, "photo.jpg");
        String key2 = photoKeyFactory.buildUploadKey(EVENT_TOKEN, GUEST_UUID, "photo.jpg");

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void belongsToGuestAndEvent_withMatchingPrefix_returnsTrue() {
        String key = "uploads/" + EVENT_TOKEN + "/" + GUEST_UUID + "/photo_abc.jpg";

        assertThat(photoKeyFactory.belongsToGuestAndEvent(key, EVENT_TOKEN, GUEST_UUID)).isTrue();
    }

    @Test
    void belongsToGuestAndEvent_withDifferentToken_returnsFalse() {
        String key = "uploads/other/" + GUEST_UUID + "/photo_abc.jpg";

        assertThat(photoKeyFactory.belongsToGuestAndEvent(key, EVENT_TOKEN, GUEST_UUID)).isFalse();
    }

    @Test
    void belongsToGuestAndEvent_withDifferentGuest_returnsFalse() {
        UUID otherUuid = UUID.fromString("99999999-9999-9999-9999-999999999999");
        String key = "uploads/" + EVENT_TOKEN + "/" + otherUuid + "/photo_abc.jpg";

        assertThat(photoKeyFactory.belongsToGuestAndEvent(key, EVENT_TOKEN, GUEST_UUID)).isFalse();
    }

    @Test
    void buildUploadKey_withTrailingDot_treatsAsNoExtension() {
        String key = photoKeyFactory.buildUploadKey(EVENT_TOKEN, GUEST_UUID, "photo.");

        assertThat(key).startsWith("uploads/" + EVENT_TOKEN + "/" + GUEST_UUID + "/photo._");
    }
}
