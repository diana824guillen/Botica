package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.LoginRequestDTO;
import com.boticavidasalud.backend.dto.LoginResponseDTO;
import com.boticavidasalud.backend.entity.Usuario;
import com.boticavidasalud.backend.repository.UsuarioRepository;
import com.boticavidasalud.backend.security.JwtUtil;
import com.boticavidasalud.backend.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequest) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(loginRequest.getUsername());
        
        // 1. Verificar si el usuario está bloqueado actualmente
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (usuario.getBloqueoHasta() != null && usuario.getBloqueoHasta().isAfter(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("mensaje", "Cuenta bloqueada temporalmente. Intente de nuevo más tarde"));
            }
        }

        try {
            // 2. Intentar autenticar
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3. Si el login es exitoso, reseteamos los intentos
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                usuario.setIntentosLogin(0);
                usuario.setBloqueoHasta(null);
                usuarioRepository.save(usuario);
            }

            // 4. Generar el token y responder
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String jwt = jwtUtil.generateToken(userDetails);

            LoginResponseDTO response = new LoginResponseDTO(
                jwt,
                userDetails.getUsername(),
                userDetails.getUsuario().getPerfil().getNombre(),
                userDetails.getUsuario().getTipoDocumento().name(),
                userDetails.getUsuario().getNumeroDocumento(),
                userDetails.getUsuario().getNombres(),
                userDetails.getUsuario().getApellidos()
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            // 5. Si la contraseña es incorrecta, sumamos un intento
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                int intentos = usuario.getIntentosLogin() + 1;
                
                if (intentos >= 3) {
                    // Bloqueo por 1 minuto
                    usuario.setBloqueoHasta(LocalDateTime.now().plusMinutes(1));
                    usuario.setIntentosLogin(0); // Reseteamos para que al desbloquear empiece de nuevo
                    usuarioRepository.save(usuario);
                    
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("mensaje", "Ha excedido el número máximo de intentos permitidos. Intente de nuevo más tarde."));
                } else {
                    usuario.setIntentosLogin(intentos);
                    usuarioRepository.save(usuario);
                    
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("mensaje", "Credenciales incorrectas. Intentos restantes: " + (3 - intentos)));
                }
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Usuario o contraseña incorrectos."));
        }
    }
}


