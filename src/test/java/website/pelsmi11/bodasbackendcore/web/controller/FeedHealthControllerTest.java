package website.pelsmi11.bodasbackendcore.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import website.pelsmi11.bodasbackendcore.TestDataFactory;
import website.pelsmi11.bodasbackendcore.web.config.SecurityConfig;
import website.pelsmi11.bodasbackendcore.web.exception.GlobalExceptionHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FeedHealthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, FeedHealthControllerTest.NoopJwtDecoderConfig.class})
class FeedHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FeedHealthController controller;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;
    @MockitoBean
    private ObjectProvider<RedisMessageListenerContainer> listenerContainerProvider;

    @TestConfiguration
    static class NoopJwtDecoderConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> TestDataFactory.jwt();
        }
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "listenerEnabled", true);
    }

    @Test
    void feedHealth_redisUp_returns200() throws Exception {
        RedisConnection connection = mock(RedisConnection.class);
        when(connection.ping()).thenReturn("PONG");
        when(redisConnectionFactory.getConnection()).thenReturn(connection);
        when(listenerContainerProvider.getIfAvailable()).thenReturn(null);

        mockMvc.perform(get("/api/v1/health/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.redis").value("UP"))
                .andExpect(jsonPath("$.data.overall").value("UP"));
    }

    @Test
    void feedHealth_redisDown_returns503() throws Exception {
        when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("connection refused"));
        when(listenerContainerProvider.getIfAvailable()).thenReturn(null);

        mockMvc.perform(get("/api/v1/health/feed"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.data.redis").value("DOWN"))
                .andExpect(jsonPath("$.data.overall").value("DOWN"));
    }

    @Test
    void feedHealth_redisReturnsNotPong_returns503() throws Exception {
        RedisConnection connection = mock(RedisConnection.class);
        when(connection.ping()).thenReturn("BUSY");
        when(redisConnectionFactory.getConnection()).thenReturn(connection);
        when(listenerContainerProvider.getIfAvailable()).thenReturn(null);

        mockMvc.perform(get("/api/v1/health/feed"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.data.redis").value("DOWN"));
    }
}
