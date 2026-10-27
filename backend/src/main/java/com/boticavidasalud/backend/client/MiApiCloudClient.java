package com.boticavidasalud.backend.client;

import com.boticavidasalud.backend.dto.ClienteResponseDTO;
import com.boticavidasalud.backend.enums.TipoDocumento;
import com.boticavidasalud.backend.exception.BusinessException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.Map;

@Component
public class MiApiCloudClient {

    private static final String MENSAJE_CUOTA_AGOTADA =
            "Se agotaron las consultas disponibles de MiApiCloud. "
            + "Puede cambiar a Registro Manual para registrar "
            + "al cliente sin consultar RENIEC.";

    private final RestTemplate restTemplate;

    @Value("${api.miapicloud.url}")
    private String apiUrl;

    @Value("${api.miapicloud.token}")
    private String apiToken;

    public MiApiCloudClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ClienteResponseDTO consultarPorDni(String dni) {

        String url = apiUrl + "/v1/dni/" + dni;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ParameterizedTypeReference<Map<String, Object>>
                    responseType =
                    new ParameterizedTypeReference<>() {};

            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            responseType
                    );

            Map<String, Object> body = response.getBody();

            if (body == null) {
                return null;
            }

            if (!Boolean.TRUE.equals(body.get("success"))) {

                if (esAgotamientoConsultas(body.toString())) {
                    throw new BusinessException(
                            MENSAJE_CUOTA_AGOTADA
                    );
                }

                return null;
            }

            Object datosObj = body.get("datos");

            if (!(datosObj instanceof Map)) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> datos =
                    (Map<String, Object>) datosObj;

            if (datos.isEmpty()) {
                return null;
            }

            Object nombresObj = datos.get("nombres");

            if (nombresObj == null
                    || nombresObj.toString().isBlank()) {
                return null;
            }

            ClienteResponseDTO cliente =
                    new ClienteResponseDTO();

            cliente.setTipoDocumento(TipoDocumento.DNI);
            cliente.setNumeroDocumento(dni);
            cliente.setNombres(nombresObj.toString().trim());

            String paterno = texto(datos.get("ape_paterno"));
            String materno = texto(datos.get("ape_materno"));

            String apellidos = (
                    (paterno == null ? "" : paterno)
                    + " "
                    + (materno == null ? "" : materno)
            ).trim();

            cliente.setApellidos(
                    apellidos.isBlank() ? null : apellidos
            );

            cliente.setEstado((short) 1);

            return cliente;

        } catch (BusinessException ex) {

            // Mantener el mensaje específico sobre las consultas agotadas.
            throw ex;

        } catch (HttpStatusCodeException ex) {

            String detalle = ex.getResponseBodyAsString();

            // HTTP 429 suele indicar que se excedió un límite de consultas.
            if (ex.getStatusCode().value() == 429
                    || esAgotamientoConsultas(detalle)) {

                throw new BusinessException(
                        MENSAJE_CUOTA_AGOTADA
                );
            }

            // Otros errores permiten ofrecer el registro manual
            return null;

        } catch (Exception ex) {
            return null;
        }
    }

    private boolean esAgotamientoConsultas(String mensaje) {

        if (mensaje == null) {
            return false;
        }

        String texto = mensaje.toLowerCase(Locale.ROOT);

        return texto.contains("cuota")
                || texto.contains("quota")
                || texto.contains("agotad")
                || texto.contains("sin consultas")
                || texto.contains("consultas disponibles")
                || texto.contains("excedid")
                || texto.contains("límite de consultas")
                || texto.contains("limite de consultas")
                || texto.contains("saldo insuficiente")
                || texto.contains("rate limit")
                || texto.contains("requests remaining");
    }

    private String texto(Object valor) {

        if (valor == null) {
            return null;
        }

        String resultado = valor.toString().trim();

        return resultado.isBlank() ? null : resultado;
    }
}


