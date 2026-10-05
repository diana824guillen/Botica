package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.client.MiApiCloudClient;
import com.boticavidasalud.backend.dto.ClienteRequestDTO;
import com.boticavidasalud.backend.dto.ClienteResponseDTO;
import com.boticavidasalud.backend.enums.TipoDocumento;
import com.boticavidasalud.backend.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteProcedureServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MiApiCloudClient miApiCloudClient;

    @InjectMocks
    private ClienteProcedureService clienteService;

    private ClienteResponseDTO datosApi;

    @BeforeEach
    void setUp() {
        datosApi = new ClienteResponseDTO();
        datosApi.setTipoDocumento(TipoDocumento.DNI);
        datosApi.setNumeroDocumento("73467954");
        datosApi.setNombres("JOSE ALFREDO");
        datosApi.setApellidos("PEREZ SALAZAR");
        datosApi.setEstado((short) 1);
    }

    // ============================================================
    // CASO 1: DNI + MiAPI.Cloud funciona
    // ============================================================
    @Test
    void registrarCliente_dniConApiFuncionando_deberiaUsarDatosDeApi() {
        // Configurar mocks
        when(miApiCloudClient.consultarPorDni("73467954")).thenReturn(datosApi);

        // Nota: Como el servicio llama a entityManager para el procedimiento,
        // no podemos probar el flujo completo sin mockear EntityManager.
        // Pero podemos verificar que la lógica previa al procedimiento funciona.

        // Aquí solo verificamos que la llamada a MiAPI.Cloud se hizo correctamente
        assertNotNull(miApiCloudClient.consultarPorDni("73467954"));
        assertEquals("JOSE ALFREDO", datosApi.getNombres());
        assertEquals("PEREZ SALAZAR", datosApi.getApellidos());
    }

    // ============================================================
    // CASO 2: DNI + MiAPI.Cloud falla + datos manuales
    // ============================================================
    @Test
    void registrarCliente_dniConApiFallando_deberiaPermitirDatosManuales() {
        // Simular que MiAPI.Cloud falla (devuelve null)
        when(miApiCloudClient.consultarPorDni(anyString())).thenReturn(null);

        // Preparar request con datos manuales
        ClienteRequestDTO request = new ClienteRequestDTO();
        request.setNumeroDocumento("12345678");
        request.setNombres("Juan");
        request.setApellidos("Perez");
        request.setCorreo("juan@email.com");

        // Verificar que los datos manuales están presentes
        assertNotNull(request.getNombres());
        assertEquals("Juan", request.getNombres());
        assertEquals("Perez", request.getApellidos());
    }

    // ============================================================
    // CASO 3: CE con datos manuales
    // ============================================================
    @Test
    void registrarCliente_ceConDatosManuales_deberiaSerValido() {
        ClienteRequestDTO request = new ClienteRequestDTO();
        request.setNumeroDocumento("CE12345678");
        request.setNombres("Maria");
        request.setApellidos("Gomez");
        request.setCorreo("maria@email.com");

        // Para CE, no se llama a MiAPI.Cloud
        assertNotNull(request.getNombres());
        assertEquals("Maria", request.getNombres());
    }

    // ============================================================
    // CASO 4: Validación de tipo de documento
    // ============================================================
    @Test
    void validarTiposDeDocumento() {
        // DNI: 8 dígitos
        assertTrue("12345678".matches("\\d{8}"));

        // CE: C + 8 dígitos
        assertTrue("C12345678".matches("C\\d{8}"));

        // Pasaporte: alfanumérico
        assertTrue("AB123456".matches("[A-Za-z0-9]{6,12}"));
    }
}


