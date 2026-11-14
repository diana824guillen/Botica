
package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.OpcionResponseDTO;
import com.boticavidasalud.backend.dto.PerfilPermisosRequestDTO;
import com.boticavidasalud.backend.dto.PerfilRequestDTO;
import com.boticavidasalud.backend.dto.PerfilResponseDTO;
import com.boticavidasalud.backend.dto.PerfilUpdateDTO;

import com.boticavidasalud.backend.entity.Opcion;
import com.boticavidasalud.backend.entity.Perfil;

import com.boticavidasalud.backend.enums.EstadoRegistro;

import com.boticavidasalud.backend.exception.BusinessException;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;

import com.boticavidasalud.backend.repository.OpcionRepository;
import com.boticavidasalud.backend.repository.PerfilRepository;
import com.boticavidasalud.backend.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class PerfilService {

    private static final String PERFIL_ADMIN = "ADMIN";

    private static final Short ESTADO_ACTIVO =
            EstadoRegistro.ACTIVO.getValor();

    private static final Short ESTADO_INACTIVO =
            EstadoRegistro.INACTIVO.getValor();

    private static final Short ESTADO_ELIMINADO =
            EstadoRegistro.ELIMINADO.getValor();

    private static final List<String> ACCIONES_CONOCIDAS = List.of(
            "EXPORTAR_EXCEL",
            "EXPORTAR_PDF",
            "RESTABLECER_CLAVE",
            "CONSULTAR_DETALLE",
            "VER_DETALLE",
            "CAMBIAR_ESTADO",
            "DESCARGAR",
            "IMPRIMIR",
            "CONSULTAR",
            "REGISTRAR",
            "ELIMINAR",
            "AJUSTAR",
            "ANULAR",
            "CREAR",
            "EDITAR",
            "LEER"
    );

    private static final Map<String, String> MODULOS = crearMapaModulos();

    private final PerfilRepository perfilRepository;
    private final OpcionRepository opcionRepository;
    private final UsuarioRepository usuarioRepository;

    public PerfilService(
            PerfilRepository perfilRepository,
            OpcionRepository opcionRepository,
            UsuarioRepository usuarioRepository) {

        this.perfilRepository = perfilRepository;
        this.opcionRepository = opcionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ============================================================
    // LISTAR PERFILES
    // ============================================================

    @Transactional(readOnly = true)
    public List<PerfilResponseDTO> listarPerfiles() {

        List<Short> estadosPermitidos = List.of(
                ESTADO_INACTIVO,
                ESTADO_ACTIVO
        );

        List<Perfil> perfiles =
                perfilRepository.findByEstadoInOrderByIdDesc(
                        estadosPermitidos
                );

        List<Opcion> opcionesActivas =
                opcionRepository.findByEstadoOrderByRutaAscNombreAsc(
                        ESTADO_ACTIVO
                );

        return perfiles.stream()
                .map(perfil -> mapearPerfil(perfil, opcionesActivas))
                .toList();
    }

    // ============================================================
    // OBTENER PERFIL POR ID
    // ============================================================

    @Transactional(readOnly = true)
    public PerfilResponseDTO obtenerPerfilPorId(Integer id) {

        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Perfil no encontrado con ID: " + id
                        )
                );

        validarPerfilNoEliminado(perfil);

        List<Opcion> opcionesActivas =
                opcionRepository.findByEstadoOrderByRutaAscNombreAsc(
                        ESTADO_ACTIVO
                );

        return mapearPerfil(perfil, opcionesActivas);
    }

    // ============================================================
    // LISTAR OPCIONES / PERMISOS DISPONIBLES
    // ============================================================

    @Transactional(readOnly = true)
    public List<OpcionResponseDTO> listarOpcionesDisponibles() {

        List<Opcion> opciones =
                opcionRepository.findByEstadoOrderByRutaAscNombreAsc(
                        ESTADO_ACTIVO
                );

        return opciones.stream()
            .map(this::mapearOpcion)
            .sorted(
                    Comparator.comparing(
                            (OpcionResponseDTO opcion) -> opcion.getModulo(),
                            Comparator.nullsLast(Comparator.<String>naturalOrder())
                    ).thenComparing(
                            (OpcionResponseDTO opcion) -> opcion.getAccion(),
                            Comparator.nullsLast(Comparator.<String>naturalOrder())
                    )
            )
            .toList();
    }

    // ============================================================
    // CREAR PERFIL
    // ============================================================

    public PerfilResponseDTO crearPerfil(PerfilRequestDTO request) {

        String nombre = normalizarNombre(request.getNombre());

        if (nombre.isBlank()) {
            throw new BusinessException(
                    "El nombre del perfil es obligatorio"
            );
        }

        validarNombreReservado(nombre);

        if (perfilRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException(
                    "Ya existe un perfil con ese nombre"
            );
        }

        Perfil perfil = new Perfil();

        perfil.setNombre(nombre);

        perfil.setDescripcion(
                normalizarDescripcion(request.getDescripcion())
        );

        perfil.setEstado(ESTADO_ACTIVO);

        // Un perfil nuevo empieza sin permisos.
        perfil.setOpciones(new LinkedHashSet<>());

        Perfil guardado = perfilRepository.save(perfil);

        List<Opcion> opcionesActivas =
                opcionRepository.findByEstadoOrderByRutaAscNombreAsc(
                        ESTADO_ACTIVO
                );

        return mapearPerfil(guardado, opcionesActivas);
    }

    // ============================================================
    // ACTUALIZAR PERFIL
    // ============================================================

    public PerfilResponseDTO actualizarPerfil(
            Integer id,
            PerfilUpdateDTO request) {

        Perfil perfil = obtenerPerfilBloqueado(id);

        validarPerfilProtegido(perfil);
        validarPerfilNoEliminado(perfil);

        String nombre = normalizarNombre(request.getNombre());

        if (nombre.isBlank()) {
            throw new BusinessException(
                    "El nombre del perfil es obligatorio"
            );
        }

        validarNombreReservado(nombre);

        perfilRepository.findByNombreIgnoreCase(nombre)
                .filter(existente ->
                        !existente.getId().equals(perfil.getId())
                )
                .ifPresent(existente -> {
                    throw new BusinessException(
                            "Ya existe un perfil con ese nombre"
                    );
                });

        perfil.setNombre(nombre);

        perfil.setDescripcion(
                normalizarDescripcion(request.getDescripcion())
        );

        Perfil actualizado = perfilRepository.save(perfil);

        List<Opcion> opcionesActivas =
                opcionRepository.findByEstadoOrderByRutaAscNombreAsc(
                        ESTADO_ACTIVO
                );

        return mapearPerfil(actualizado, opcionesActivas);
    }

    // ============================================================
    // ACTUALIZAR PERMISOS DE UN PERFIL
    // ============================================================

    
    public PerfilResponseDTO actualizarPermisos(
                Integer id,
                PerfilPermisosRequestDTO request) {

        Perfil perfil = obtenerPerfilBloqueado(id);

        validarPerfilProtegido(perfil);
        validarPerfilNoEliminado(perfil);

        if (request == null || request.getOpcionIds() == null) {
                throw new BusinessException(
                        "Debe seleccionar los módulos del perfil"
                );
        }

        Set<Integer> idsSolicitados =
                new LinkedHashSet<>(request.getOpcionIds());

        Set<String> modulosPermitidos = Set.of(
                "CLIENTE_ACCESO",
                "CATEGORIA_ACCESO",
                "PRODUCTO_ACCESO",
                "LOTE_ACCESO",
                "PUNTO_VENTA_ACCESO",
                "HISTORIAL_VENTAS_ACCESO",
                "DESCUENTO_ACCESO",
                "MOVIMIENTO_INVENTARIO_ACCESO"
        );

        List<Opcion> opcionesActivas =
                opcionRepository.findByEstadoOrderByRutaAscNombreAsc(
                        ESTADO_ACTIVO
                );

        Map<Integer, Opcion> opcionesPorId = new HashMap<>();

        for (Opcion opcion : opcionesActivas) {

                if (opcion != null && opcion.getId() != null) {
                opcionesPorId.put(opcion.getId(), opcion);
                }
        }

        for (Integer opcionId : idsSolicitados) {

                Opcion opcion = opcionesPorId.get(opcionId);

                if (opcion == null ||
                        !modulosPermitidos.contains(opcion.getNombre())) {

                throw new BusinessException(
                        "Uno o más módulos seleccionados no están disponibles"
                );
                }
        }

        Set<Opcion> nuevasOpciones = new LinkedHashSet<>();

        for (Integer opcionId : idsSolicitados) {
                nuevasOpciones.add(opcionesPorId.get(opcionId));
        }

        if (perfil.getOpciones() == null) {
                perfil.setOpciones(new LinkedHashSet<>());
        } else {
                perfil.getOpciones().clear();
        }

        perfil.getOpciones().addAll(nuevasOpciones);

        Perfil actualizado = perfilRepository.save(perfil);

        return mapearPerfil(actualizado, opcionesActivas);
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR PERFIL
    // ============================================================

    public PerfilResponseDTO cambiarEstado(
            Integer id,
            Short nuevoEstado) {

        if (nuevoEstado == null
                || (!nuevoEstado.equals(ESTADO_ACTIVO)
                && !nuevoEstado.equals(ESTADO_INACTIVO))) {

            throw new BusinessException(
                    "Estado inválido. Debe ser 0 (Inactivo) "
                    + "o 1 (Activo)"
            );
        }

        Perfil perfil = obtenerPerfilBloqueado(id);

        validarPerfilProtegido(perfil);
        validarPerfilNoEliminado(perfil);

        if (perfil.getEstado().equals(nuevoEstado)) {
            return obtenerRespuestaActual(perfil);
        }

        // No desactivar perfiles que todavía tengan usuarios asociados.
        if (nuevoEstado.equals(ESTADO_INACTIVO)) {

            long totalUsuarios =
                    usuarioRepository.countByPerfil_Id(id);

            if (totalUsuarios > 0) {
                throw new BusinessException(
                        "No se puede desactivar este perfil porque "
                        + "todavía tiene usuarios asociados. "
                        + "Primero debe reasignar esos usuarios."
                );
            }

            perfil.setFechaDesactivacion(LocalDateTime.now());

        } else {

            perfil.setFechaDesactivacion(null);
        }

        perfil.setEstado(nuevoEstado);

        Perfil actualizado = perfilRepository.save(perfil);

        return obtenerRespuestaActual(actualizado);
    }

    // ============================================================
    // ELIMINACIÓN LÓGICA
    // ============================================================

    public void eliminarLogicamente(Integer id) {

        Perfil perfil = obtenerPerfilBloqueado(id);

        validarPerfilProtegido(perfil);
        validarPerfilNoEliminado(perfil);

        long totalUsuarios =
                usuarioRepository.countByPerfil_Id(id);

        if (totalUsuarios > 0) {
            throw new BusinessException(
                    "No se puede eliminar este perfil porque "
                    + "tiene usuarios asociados. "
                    + "Primero debe reasignar esos usuarios."
            );
        }

        perfil.setEstado(ESTADO_ELIMINADO);
        perfil.setFechaEliminacion(LocalDateTime.now());

        perfilRepository.save(perfil);
    }

    // ============================================================
    // OBTENER PERFIL CON BLOQUEO
    // ============================================================

    private Perfil obtenerPerfilBloqueado(Integer id) {

        return perfilRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Perfil no encontrado con ID: " + id
                        )
                );
    }

    // ============================================================
    // GENERAR RESPUESTA ACTUAL
    // ============================================================

    private PerfilResponseDTO obtenerRespuestaActual(Perfil perfil) {

        List<Opcion> opcionesActivas =
                opcionRepository.findByEstadoOrderByRutaAscNombreAsc(
                        ESTADO_ACTIVO
                );

        return mapearPerfil(perfil, opcionesActivas);
    }

    // ============================================================
    // MAPEAR PERFIL A DTO
    // ============================================================

    private PerfilResponseDTO mapearPerfil(
            Perfil perfil,
            List<Opcion> opcionesActivas) {

        Set<Integer> opcionIds = new LinkedHashSet<>();

        // ADMIN tiene acceso efectivo a todas las opciones activas.
        if (esPerfilAdmin(perfil)) {

            for (Opcion opcion : opcionesActivas) {

                if (opcion != null && opcion.getId() != null) {
                    opcionIds.add(opcion.getId());
                }
            }

        } else if (perfil.getOpciones() != null) {

            perfil.getOpciones().stream()
                    .filter(opcion -> opcion != null)
                    .filter(opcion ->
                            ESTADO_ACTIVO.equals(opcion.getEstado())
                    )
                    .filter(opcion -> opcion.getId() != null)
                    .forEach(opcion ->
                            opcionIds.add(opcion.getId())
                    );
        }

        List<Integer> idsOrdenados = opcionIds.stream()
                .sorted()
                .toList();

        long totalUsuarios =
                usuarioRepository.countByPerfil_Id(perfil.getId());

        return new PerfilResponseDTO(
                perfil.getId(),
                perfil.getNombre(),
                perfil.getDescripcion(),
                perfil.getEstado(),
                totalUsuarios,
                idsOrdenados.size(),
                idsOrdenados,
                esPerfilAdmin(perfil)
        );
    }

    // ============================================================
    // MAPEAR OPCIÓN A DTO
    // ============================================================

    private OpcionResponseDTO mapearOpcion(Opcion opcion) {

        DatosPermiso datos =
                obtenerDatosPermiso(opcion.getNombre());

        String nombreModulo = MODULOS.getOrDefault(
                datos.moduloClave(),
                formatearTexto(datos.moduloClave())
        );

        String nombreAccion =
                formatearAccion(datos.accionClave());

        return new OpcionResponseDTO(
                opcion.getId(),
                opcion.getNombre(),
                opcion.getRuta(),
                opcion.getIcono(),
                nombreModulo,
                nombreAccion,
                opcion.getEstado()
        );
    }

    // ============================================================
    // OBTENER MÓDULO Y ACCIÓN DEL PERMISO
    // ============================================================

    private DatosPermiso obtenerDatosPermiso(String nombrePermiso) {

        String nombre = nombrePermiso
                .trim()
                .toUpperCase(Locale.ROOT);

        for (String accion : ACCIONES_CONOCIDAS) {

            String sufijo = "_" + accion;

            if (nombre.endsWith(sufijo)) {

                String moduloClave = nombre.substring(
                        0,
                        nombre.length() - sufijo.length()
                );

                return new DatosPermiso(
                        moduloClave,
                        accion
                );
            }
        }

        int separador = nombre.lastIndexOf('_');

        if (separador > 0 && separador < nombre.length() - 1) {

            return new DatosPermiso(
                    nombre.substring(0, separador),
                    nombre.substring(separador + 1)
            );
        }

        return new DatosPermiso(
                nombre,
                "ACCEDER"
        );
    }

    // ============================================================
    // FORMATEAR ACCIONES
    // ============================================================

    private String formatearAccion(String accion) {

        return switch (accion) {

            case "LEER", "CONSULTAR" ->
                    "Consultar";

            case "CREAR", "REGISTRAR" ->
                    "Registrar";

            case "EDITAR" ->
                    "Editar";

            case "ELIMINAR" ->
                    "Eliminar";

            case "AJUSTAR" ->
                    "Ajustar";

            case "CAMBIAR_ESTADO" ->
                    "Cambiar estado";

            case "RESTABLECER_CLAVE" ->
                    "Restablecer contraseña";

            case "VER_DETALLE", "CONSULTAR_DETALLE" ->
                    "Ver detalle";

            case "DESCARGAR" ->
                    "Descargar";

            case "IMPRIMIR" ->
                    "Imprimir";

            case "ANULAR" ->
                    "Anular";

            case "EXPORTAR_PDF" ->
                    "Exportar PDF";

            case "EXPORTAR_EXCEL" ->
                    "Exportar Excel";

            default ->
                    formatearTexto(accion);
        };
    }

    // ============================================================
    // MAPA DE NOMBRES DE MÓDULOS
    // ============================================================

    private static Map<String, String> crearMapaModulos() {

        Map<String, String> modulos = new LinkedHashMap<>();

        modulos.put("CLIENTE", "Clientes");
        modulos.put("CATEGORIA", "Categorías");
        modulos.put("PRODUCTO", "Productos");
        modulos.put("LOTE", "Lotes");

        modulos.put("PUNTO_VENTA", "Punto de venta");
        modulos.put("VENTA", "Ventas");
        modulos.put("HISTORIAL_VENTAS", "Historial de ventas");
        modulos.put("DESCUENTO", "Descuentos");

        modulos.put("USUARIO", "Usuarios");
        modulos.put("PERFIL", "Perfiles");

        modulos.put(
                "MOVIMIENTO_INVENTARIO",
                "Movimientos de inventario"
        );

        modulos.put("REPORTE", "Reportes");

        return Map.copyOf(modulos);
    }

    // ============================================================
    // FORMATEAR TEXTO
    // ============================================================

    private String formatearTexto(String texto) {

        if (texto == null || texto.isBlank()) {
            return "";
        }

        String[] palabras = texto
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .split("\\s+");

        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {

            if (palabra.isBlank()) {
                continue;
            }

            if (resultado.length() > 0) {
                resultado.append(" ");
            }

            resultado.append(
                    palabra.substring(0, 1).toUpperCase(Locale.ROOT)
            );

            if (palabra.length() > 1) {
                resultado.append(palabra.substring(1));
            }
        }

        return resultado.toString();
    }

    // ============================================================
    // NORMALIZAR NOMBRE
    // ============================================================

    private String normalizarNombre(String nombre) {

        if (nombre == null) {
            return "";
        }

        return nombre.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    // ============================================================
    // NORMALIZAR DESCRIPCIÓN
    // ============================================================

    private String normalizarDescripcion(String descripcion) {

        if (descripcion == null) {
            return null;
        }

        String resultado = descripcion.trim();

        return resultado.isEmpty() ? null : resultado;
    }

    // ============================================================
    // VALIDAR NOMBRE RESERVADO
    // ============================================================

    private void validarNombreReservado(String nombre) {

        if (PERFIL_ADMIN.equalsIgnoreCase(nombre)) {
            throw new BusinessException(
                    "El nombre ADMIN está reservado "
                    + "para el perfil administrativo principal"
            );
        }
    }

    // ============================================================
    // IDENTIFICAR PERFIL ADMIN
    // ============================================================

    private boolean esPerfilAdmin(Perfil perfil) {

        return perfil != null
                && perfil.getNombre() != null
                && PERFIL_ADMIN.equalsIgnoreCase(perfil.getNombre());
    }

    // ============================================================
    // PROTEGER PERFIL ADMIN
    // ============================================================

    private void validarPerfilProtegido(Perfil perfil) {

        if (esPerfilAdmin(perfil)) {
            throw new BusinessException(
                    "El perfil ADMIN está protegido y no puede "
                    + "ser modificado, desactivado ni eliminado"
            );
        }
    }

    // ============================================================
    // VALIDAR QUE EL PERFIL NO ESTÉ ELIMINADO
    // ============================================================

    private void validarPerfilNoEliminado(Perfil perfil) {

        if (ESTADO_ELIMINADO.equals(perfil.getEstado())) {
            throw new BusinessException(
                    "No se puede operar con un perfil eliminado"
            );
        }
    }

    // ============================================================
    // DATOS INTERNOS DE UN PERMISO
    // ============================================================

    private record DatosPermiso(
            String moduloClave,
            String accionClave
    ) {
    }
}


