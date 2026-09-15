package website.pelsmi11.bodasbackendcore.web.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps the {@code cognito:groups} claim to Spring Security authorities:
 * group {@code ADMIN} → {@code ROLE_ADMIN}; absence of {@code ADMIN} (including
 * GUEST-only or no claim) → no admin authority (treated as guest).
 */
public class CognitoGroupsAuthoritiesConverter implements Converter<Jwt, List<GrantedAuthority>> {

    public static final String ADMIN_GROUP = "ADMIN";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Override
    public List<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        if (groups != null && groups.contains(ADMIN_GROUP)) {
            authorities.add(new SimpleGrantedAuthority(ROLE_ADMIN));
        }
        return authorities;
    }
}
