package com.earacg.earaconnect.config;

import com.earacg.earaconnect.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // Set FRONTEND_URL in Railway env vars, e.g. https://front-end-sandra.vercel.app
    @Value("${frontend.url:https://front-end-sandra.vercel.app}")
    private String frontendUrl;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless JWT APIs
            .csrf(csrf -> csrf.disable())

            // CORS — allow Vercel frontend
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless — no HttpSession created or used
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Public endpoints — everything else requires a valid JWT
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/logout",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/auth/verify-reset-token",
                    "/uploads/**"
                ).permitAll()
                .anyRequest().authenticated()
            )

            // Plug in the JWT filter before Spring's username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow your Vercel frontend — add more origins if needed
        config.setAllowedOrigins(List.of(
            frontendUrl,
            "http://localhost:3000",
            "http://localhost:5173"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        // Don't need credentials:true since we're using Authorization header, not cookies
        config.setAllowCredentials(false);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}