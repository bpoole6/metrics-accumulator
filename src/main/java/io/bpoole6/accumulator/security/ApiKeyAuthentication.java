package io.bpoole6.accumulator.security;

import io.bpoole6.accumulator.service.metricgroup.Group;
import java.util.Collection;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {
    private final String apiKey;
    private final Group metricGroup;
    public ApiKeyAuthentication(String apiKey, Group metricGroup, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        this.metricGroup = metricGroup;

        setAuthenticated(Objects.nonNull(metricGroup));
    }

    @Override
    public Object getCredentials() {
        return metricGroup;
    }

    @Override
    public Object getPrincipal() {
        return apiKey;
    }
}