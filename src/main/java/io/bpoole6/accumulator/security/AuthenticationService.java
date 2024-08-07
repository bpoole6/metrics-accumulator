package io.bpoole6.accumulator.security;

import io.bpoole6.accumulator.service.metricgroup.Group;
import io.bpoole6.accumulator.service.MetricGroupConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

public class AuthenticationService {

    private static final String AUTH_TOKEN_HEADER_NAME = "X-API-KEY";
    public static Authentication getAuthentication(HttpServletRequest request,
        MetricGroupConfiguration metricGroupConfiguration) {
        String apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME);
        Group group = metricGroupConfiguration.getMetricGroupByApiKey().get(apiKey);
        return new ApiKeyAuthentication(apiKey, group, AuthorityUtils.NO_AUTHORITIES);
    }


}