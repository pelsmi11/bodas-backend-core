package website.pelsmi11.bodasbackendcore;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.model.EventStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.ModerationStatus;
import website.pelsmi11.bodasbackendcore.persistence.model.Photo;
import website.pelsmi11.bodasbackendcore.persistence.model.User;
import website.pelsmi11.bodasbackendcore.persistence.model.UserDevice;
import website.pelsmi11.bodasbackendcore.persistence.model.UserRole;

/**
 * Shared builders for test entities. All builders use sensible defaults so tests
 * only need to override the fields relevant to the scenario.
 */
public final class TestDataFactory {

    public static final String TEST_ADMIN_SUB = "test-admin-sub";
    public static final String TEST_EVENT_TOKEN = "ev123456";
    public static final UUID TEST_GUEST_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID TEST_PHOTO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private TestDataFactory() {
    }

    public static Jwt jwt() {
        return jwt(TEST_ADMIN_SUB);
    }

    public static Jwt jwt(String sub) {
        return Jwt.withTokenValue("test-jwt-token")
                .header("alg", "RS256")
                .claim("sub", sub)
                .issuer("https://example.com/test-issuer")
                .build();
    }

    public static Event event() {
        return event(1, TEST_EVENT_TOKEN, true);
    }

    public static Event event(Integer id, String token, boolean active) {
        Event event = new Event();
        event.setId(id);
        event.setName("Boda Test");
        event.setToken(token);
        event.setAdminId(TEST_ADMIN_SUB);
        event.setStatus(EventStatus.ACTIVE);
        event.setIsActive(active);
        event.setCreatedAt(OffsetDateTime.now());
        return event;
    }

    public static User user() {
        return user(1, "cognito-user-1");
    }

    public static User user(Integer id, String cognitoId) {
        User user = new User();
        user.setId(id);
        user.setCognitoId(cognitoId);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setRole(UserRole.GUEST);
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }

    public static UserDevice userDevice() {
        return userDevice(TEST_GUEST_UUID, user(), false);
    }

    public static UserDevice userDevice(UUID guestUuid, User user, boolean blocked) {
        UserDevice device = new UserDevice();
        device.setGuestUuid(guestUuid);
        device.setUser(user);
        device.setBlocked(blocked);
        device.setLastActive(OffsetDateTime.now());
        if (blocked) {
            device.setBlockedAt(OffsetDateTime.now());
        }
        return device;
    }

    public static Photo photo() {
        return photo(TEST_PHOTO_ID, event(), ModerationStatus.PENDING);
    }

    public static Photo photo(UUID id, Event event, ModerationStatus status) {
        User user = user();
        UserDevice device = userDevice(TEST_GUEST_UUID, user, false);
        Photo photo = new Photo();
        photo.setId(id);
        photo.setEvent(event);
        photo.setUser(user);
        photo.setDevice(device);
        photo.setS3Key("uploads/" + event.getToken() + "/" + TEST_GUEST_UUID + "/photo_abc123.jpg");
        photo.setStatus(status);
        photo.setUploadedAt(OffsetDateTime.now());
        return photo;
    }
}
