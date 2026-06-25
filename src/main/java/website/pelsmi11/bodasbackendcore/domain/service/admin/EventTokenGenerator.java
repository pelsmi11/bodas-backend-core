package website.pelsmi11.bodasbackendcore.domain.service.admin;

import org.springframework.stereotype.Component;
import website.pelsmi11.bodasbackendcore.persistence.repository.EventRepository;

import java.security.SecureRandom;

/**
 * Generates short, URL-friendly unique tokens for event access (QR codes).
 */
@Component
public class EventTokenGenerator {

    private static final int TOKEN_LENGTH = 8;
    private static final int MAX_ATTEMPTS = 10;
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final SecureRandom random = new SecureRandom();
    private final EventRepository eventRepository;

    public EventTokenGenerator(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Generates a unique token, retrying on collision.
     */
    public String generateUniqueToken() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = randomToken();
            if (!eventRepository.existsByToken(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique token after " + MAX_ATTEMPTS + " attempts");
    }

    private String randomToken() {
        char[] buffer = new char[TOKEN_LENGTH];
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            buffer[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(buffer);
    }
}
