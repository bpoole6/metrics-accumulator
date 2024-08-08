package io.bpoole6.accumulator.security;

import io.bpoole6.accumulator.service.MetricsAccumulatorConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class CustomWebSecurityConfigurerAdapter {
    private final MetricsAccumulatorConfiguration metricsAccumulatorConfiguration;

    public CustomWebSecurityConfigurerAdapter(MetricsAccumulatorConfiguration metricsAccumulatorConfiguration) {
        this.metricsAccumulatorConfiguration = metricsAccumulatorConfiguration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers(HttpMethod.POST).authenticated()
                                .anyRequest().permitAll())

                .httpBasic(Customizer.withDefaults())
                .sessionManagement(httpSecuritySessionManagementConfigurer -> httpSecuritySessionManagementConfigurer.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .addFilterBefore(new AuthenticationFilter(this.metricsAccumulatorConfiguration), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}