package com.boticavidasalud.backend.config;

import com.boticavidasalud.backend.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            UserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http) throws Exception {

        AuthenticationManagerBuilder authBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());

        return authBuilder.build();
    }

    // CONFIGURACIÓN DE CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                Arrays.asList("http://localhost:4200")
        );

        configuration.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type", "Cache-Control")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    // CONFIGURACIÓN PRINCIPAL DE SEGURIDAD
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

            .cors(cors ->
                    cors.configurationSource(corsConfigurationSource())
            )

            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                    .requestMatchers(
                            "/api/auth/login",
                            "/swagger-ui/**",
                            "/v3/api-docs/**"
                    ).permitAll()

                    .anyRequest().authenticated()
            )

            // Respuestas diferenciadas para autenticación y permisos.
            .exceptionHandling(exceptions -> exceptions

                    .authenticationEntryPoint((request, response, ex) -> {

                        escribirRespuesta(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Unauthorized",
                                "Debes iniciar sesión para acceder a este recurso."
                        );
                    })

                    .accessDeniedHandler((request, response, ex) -> {

                        escribirRespuesta(
                                response,
                                HttpServletResponse.SC_FORBIDDEN,
                                "Forbidden",
                                "No tienes permisos para realizar esta acción."
                        );
                    })
            )

            // Autenticación sin sesiones del servidor.
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    private void escribirRespuesta(
            HttpServletResponse response,
            int status,
            String error,
            String mensaje) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"status\":" + status
                + ",\"error\":\"" + error + "\""
                + ",\"mensaje\":\"" + mensaje + "\"}"
        );
    }
}


