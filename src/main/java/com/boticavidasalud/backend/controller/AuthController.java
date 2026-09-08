package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.LoginRequest;
import com.boticavidasalud.backend.dto.LoginResponse;
import com.boticavidasalud.backend.security.JwtUtil;
import com.boticavidasalud.backend.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        // Autenticar al usuario
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Obtener los datos del usuario autenticado
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Generar el token JWT
        String jwt = jwtUtil.generateToken(userDetails);

        // Respuesta
        LoginResponse response = new LoginResponse(
            jwt,
            userDetails.getUsername(),
            userDetails.getUsuario().getPerfil().getNombre(),
            userDetails.getUsuario().getTipoDocumento().name(),
            userDetails.getUsuario().getNumeroDocumento(),
            userDetails.getUsuario().getNombres(),
            userDetails.getUsuario().getApellidos()
        );

        return ResponseEntity.ok(response);
    }
}


