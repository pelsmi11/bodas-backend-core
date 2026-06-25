package website.pelsmi11.bodasbackendcore.domain.service.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import website.pelsmi11.bodasbackendcore.persistence.repository.EventRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventTokenGeneratorTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventTokenGenerator tokenGenerator;

    @Test
    void generateUniqueToken_noCollision_returns8CharAlphanumeric() {
        when(eventRepository.existsByToken(anyString())).thenReturn(false);

        String token = tokenGenerator.generateUniqueToken();

        assertThat(token).hasSize(8);
        assertThat(token).matches("[A-Za-z0-9]+");
    }

    @Test
    void generateUniqueToken_collisionThenSuccess_retriesAndReturnsUnique() {
        when(eventRepository.existsByToken(anyString()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        String token = tokenGenerator.generateUniqueToken();

        assertThat(token).hasSize(8);
    }

    @Test
    void generateUniqueToken_allAttemptsCollide_throwsIllegalState() {
        when(eventRepository.existsByToken(anyString())).thenReturn(true);

        assertThatThrownBy(() -> tokenGenerator.generateUniqueToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unique token");
    }

    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
