package com.boticavidasalud.backend.security;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            UserDetailsService userDetailsService) {

        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Si no existe un token, Spring Security determinará si la ruta necesita autenticación.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7).trim();

        try {

            if (jwt.isBlank()) {
                responderNoAutenticado(response);
                return;
            }

            // Extraer el usuario asociado al token.
            String username = jwtUtil.extractUsername(jwt);

            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                // Comprobar que la cuenta siga habilitada.
                if (!userDetails.isEnabled()) {

                    SecurityContextHolder.clearContext();

                    responderNoAutenticado(response);
                    return;
                }

                // Validar la firma y la fecha de expiración.
                if (!jwtUtil.validateToken(jwt, userDetails)) {

                    SecurityContextHolder.clearContext();

                    responderNoAutenticado(response);
                    return;
                }

                // Registrar la autenticación.
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
            }

        } catch (JwtException | IllegalArgumentException ex) {

            // Token vencido, manipulado o inválido.
            SecurityContextHolder.clearContext();

            responderNoAutenticado(response);
            return;

        } catch (UsernameNotFoundException ex) {

            // El usuario asociado al token ya no existe.
            SecurityContextHolder.clearContext();

            responderNoAutenticado(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void responderNoAutenticado(
            HttpServletResponse response) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"status\":401,"
                + "\"error\":\"Unauthorized\","
                + "\"mensaje\":\"La sesión ha vencido o no es válida. "
                + "Inicia sesión nuevamente.\"}"
        );
    }
}


