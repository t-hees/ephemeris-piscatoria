package com.tadeo.ephemeris_piscatoria.config;

import com.tadeo.ephemeris_piscatoria.service.UserService;
import com.tadeo.ephemeris_piscatoria.util.CsrfCookieFilter;
import com.tadeo.ephemeris_piscatoria.util.JwtAuthFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@Profile("!no_auth")
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserService userService;
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            // The auth cookie is sent by the browser automatically, so writes need the XSRF-TOKEN cookie echoed as header.
            // Login and register don't use the auth cookie and only accept json, logout only clears it
            .csrf(csrf -> csrf
                .spa()
                .ignoringRequestMatchers("/api/auth/**"))
            .httpBasic(http -> http.disable())
            .formLogin(form -> form.disable())
            .authorizeHttpRequests(
                authorizeRequest -> authorizeRequest
                    // Logout only clears the cookie, so it also works with an expired token
                    .requestMatchers("/api/auth/**").permitAll()
                    .anyRequest().authenticated()
            )
            // Missing or invalid auth cookies are 401 instead of spring security's default 403.
            // Csrf failures set their 403 directly, the default error forward to /error would turn them into 401
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, e) -> response.setStatus(HttpStatus.FORBIDDEN.value())))
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
