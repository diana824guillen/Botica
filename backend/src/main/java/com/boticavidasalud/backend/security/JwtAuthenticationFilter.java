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

        // Obtener el encabezado de autorización.
        final String authHeader = request.getHeader("Authorization");

        // Si no existe un token, continuar.
        // Spring Security determinará si la ruta requiere autenticación.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7).trim();

        try {

            if (jwt.isBlank()) {
                responderNoAutenticado(
                        response,
                        "La sesión ha vencido o no es válida. Inicia sesión nuevamente."
                );
                return;
            }

            // Extraer el usuario del token.
            String username = jwtUtil.extractUsername(jwt);

            // Validar el token si todavía no existe autenticación.
            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                // Comprobar que la cuenta siga habilitada.
                if (!userDetails.isEnabled()) {

                    SecurityContextHolder.clearContext();

                    responderNoAutenticado(
                            response,
                            "La cuenta no está habilitada. Contacta con la administradora."
                    );

                    return;
                }

                // Comprobar que el token sea válido y no haya vencido.
                if (!jwtUtil.validateToken(jwt, userDetails)) {

                    SecurityContextHolder.clearContext();

                    responderNoAutenticado(
                            response,
                            "La sesión ha vencido o no es válida. Inicia sesión nuevamente."
                    );

                    return;
                }

                // Registrar la autenticación del usuario.
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

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (JwtException | IllegalArgumentException ex) {

            // Token vencido, manipulado, mal formado o inválido.
            SecurityContextHolder.clearContext();

            responderNoAutenticado(
                    response,
                    "La sesión ha vencido o no es válida. Inicia sesión nuevamente."
            );

            return;

        } catch (UsernameNotFoundException ex) {

            // El usuario asociado al token ya no existe.
            SecurityContextHolder.clearContext();

            responderNoAutenticado(
                    response,
                    "La cuenta asociada a esta sesión ya no está disponible. Inicia sesión nuevamente."
            );

            return;
        }

        filterChain.doFilter(request, response);
    }

    // Generar una respuesta 401 cuando no existe una autenticación válida.
    private void responderNoAutenticado(
            HttpServletResponse response,
            String mensaje) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"status\":401,"
                + "\"error\":\"Unauthorized\","
                + "\"mensaje\":\"" + mensaje + "\"}"
        );
    }
}


