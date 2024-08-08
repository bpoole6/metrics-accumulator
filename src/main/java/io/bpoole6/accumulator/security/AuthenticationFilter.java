package io.bpoole6.accumulator.security;

import io.bpoole6.accumulator.service.MetricsAccumulatorConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

public class AuthenticationFilter extends GenericFilterBean {

    private final MetricsAccumulatorConfiguration metricsAccumulatorConfiguration;

    public AuthenticationFilter(MetricsAccumulatorConfiguration metricsAccumulatorConfiguration) {

      this.metricsAccumulatorConfiguration = metricsAccumulatorConfiguration;
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {
        try {
            Authentication authentication = AuthenticationService.getAuthentication((HttpServletRequest) request, metricsAccumulatorConfiguration);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception exp) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            PrintWriter writer = httpResponse.getWriter();
            writer.print(exp.getMessage());
            writer.flush();
            writer.close();
        }

        filterChain.doFilter(request, response);
    }


}