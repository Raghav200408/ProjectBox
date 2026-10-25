package com.boxproject.BoxProject.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.boxproject.BoxProject.repository.UserRepository;
import com.boxproject.BoxProject.security.JwtAuthenticationFilter;
import com.boxproject.BoxProject.security.JwtUtil;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    UrlBasedCorsConfigurationSource corsConfigurationSource, JwtUtil jwt, UserRepository users) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(jwt, users), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/players/**").hasRole("PLAYER")
                        .requestMatchers(HttpMethod.POST, "/api/users/appOwner", "/api/users/venueOwner").hasRole("APP_OWNER")
                        .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/player", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/all").hasRole("APP_OWNER")
                        .requestMatchers("/api/users/**").access((authentication, context) -> {
                            var account = authentication.get();
                            boolean admin = account.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_APP_OWNER"));
                            String uri = context.getRequest().getRequestURI();
                            String id = uri.substring(uri.lastIndexOf('/') + 1);
                            return new AuthorizationDecision(account.isAuthenticated() && (admin || account.getName().equals(id)));
                        })
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, error) -> response.sendError(401))
                        .accessDeniedHandler((request, response, error) -> response.sendError(403)))
                .build();
    }
}
