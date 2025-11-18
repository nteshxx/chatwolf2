package com.chatwolf.auth.config;

import com.chatwolf.auth.exception.AccessDeniedExceptionHandler;
import com.chatwolf.auth.exception.AuthFailedExceptionHandler;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private AuthFailedExceptionHandler authFailedExceptionHandler;
    private AccessDeniedExceptionHandler accessDeniedExceptionHandler;

    @Bean
    AccessTokenFilter accessTokenFilter() {
        return new AccessTokenFilter();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/token",
                                "/api/auth/validate",
                                "/api/auth/.well-known/jwks.json",
                                "/actuator/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .cors((cors) -> cors.disable())
                .csrf((csrf) -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authFailedExceptionHandler)
                        .accessDeniedHandler(accessDeniedExceptionHandler))
                .addFilterBefore(accessTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManagerBean(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
