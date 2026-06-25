package website.pelsmi11.bodasbackendcore.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/photos/presigned-url").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/photos/confirm").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/moderation").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*/feed").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*/stream").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/health/feed").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/admin/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
