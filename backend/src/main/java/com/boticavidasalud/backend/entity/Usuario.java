package com.boticavidasalud.backend.entity;

import com.boticavidasalud.backend.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_perfil", nullable = false)
    private Perfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento = TipoDocumento.DNI;

    @Column(name = "numero_documento", length = 15, nullable = false, unique = true)
    private String numeroDocumento;

    @Column(length = 100, nullable = false)
    private String nombres;

    @Column(length = 100, nullable = false)
    private String apellidos;

    @Column(length = 50, nullable = false, unique = true)
    private String username;

    @Column(length = 255, nullable = false)
    private String clave;   // BCrypt hash

    @Column(length = 255)
    private String correo;

    @Column(nullable = false)
    private Short estado = 1;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_desactivacion")
    private LocalDateTime fechaDesactivacion;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    @Column(name = "intentos_login", nullable = false)
    private Integer intentosLogin = 0;

    @Column(name = "bloqueo_hasta")
    private LocalDateTime bloqueoHasta;

    @Column(name = "ultimo_intento")
    private LocalDateTime ultimoIntento;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (estado == null) estado = 1;
        if (intentosLogin == null) intentosLogin = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}


