package website.pelsmi11.bodasbackendcore.web.auth;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Jwt → authorities conversion wired into the resource-server DSL:
 * {@code cognito:groups} claim → Spring authorities (ADMIN → ROLE_ADMIN;
 * absence of ADMIN → guest).
 */
public final class CognitoJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final CognitoGroupsAuthoritiesConverter groupsConverter = new CognitoGroupsAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = groupsConverter.convert(jwt);
        return new JwtAuthenticationToken(jwt, authorities.stream().collect(Collectors.toList()));
    }
}
