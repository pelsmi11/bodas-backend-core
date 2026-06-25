package website.pelsmi11.bodasbackendcore.domain.service.photo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;
import website.pelsmi11.bodasbackendcore.persistence.model.Event;
import website.pelsmi11.bodasbackendcore.persistence.repository.EventRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventResolverTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventResolver eventResolver;

    @Test
    void findActiveEvent_whenActive_returnsEvent() {
        Event event = TestDataFactory.event();
        when(eventRepository.findFirstByToken("ev123456")).thenReturn(Optional.of(event));

        Event result = eventResolver.findActiveEvent("ev123456");

        assertThat(result).isSameAs(event);
    }

    @Test
    void findActiveEvent_whenInactive_throws404() {
        Event inactiveEvent = TestDataFactory.event(1, "ev123456", false);
        when(eventRepository.findFirstByToken("ev123456")).thenReturn(Optional.of(inactiveEvent));

        assertThatThrownBy(() -> eventResolver.findActiveEvent("ev123456"))
                .isInstanceOf(CustomErrorException.class)
                .hasMessage("Event not found or inactive");
    }

    @Test
    void findActiveEvent_whenNotFound_throws404() {
        when(eventRepository.findFirstByToken("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventResolver.findActiveEvent("nonexistent"))
                .isInstanceOf(CustomErrorException.class)
                .hasMessage("Event not found or inactive");
    }

    @Test
    void findActiveEvent_whenIsActiveNull_throws404() {
        Event event = TestDataFactory.event();
        event.setIsActive(null);
        when(eventRepository.findFirstByToken("ev123456")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventResolver.findActiveEvent("ev123456"))
                .isInstanceOf(CustomErrorException.class);
    }
}
