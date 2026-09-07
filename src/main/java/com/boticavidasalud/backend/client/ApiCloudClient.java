package com.boticavidasalud.backend.client;

import com.boticavidasalud.backend.dto.ClienteResponseDTO;
import com.boticavidasalud.backend.enums.TipoDocumento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class ApiCloudClient {

    private final RestTemplate restTemplate;

    @Value("${api.apicloud.url}")
    private String apiUrl;

    @Value("${api.apicloud.token}")
    private String apiToken;

    // Constructor para inyección de dependencias (buena práctica)
    public ApiCloudClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ClienteResponseDTO consultarPorDni(String dni) {
        String url = apiUrl + "/persona/dni/" + dni;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        headers.set("Content-Type", "application/json");

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            // Usamos ParameterizedTypeReference para evitar raw types y cast
            ParameterizedTypeReference<Map<String, Object>> responseType =
                    new ParameterizedTypeReference<Map<String, Object>>() {};

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    responseType
            );

            Map<String, Object> body = response.getBody();
            if (body == null || body.isEmpty()) {
                return null;
            }

            ClienteResponseDTO cliente = new ClienteResponseDTO();
            cliente.setTipoDocumento(TipoDocumento.DNI);
            cliente.setNumeroDocumento(dni);

            // Ajusta las claves según la documentación real de ApiCloud
            if (body.containsKey("nombres")) {
                cliente.setNombres((String) body.get("nombres"));
            } else if (body.containsKey("names")) {
                cliente.setNombres((String) body.get("names"));
            }

            if (body.containsKey("apellidos")) {
                cliente.setApellidos((String) body.get("apellidos"));
            } else if (body.containsKey("surnames")) {
                cliente.setApellidos((String) body.get("surnames"));
            }

            if (body.containsKey("correo")) {
                cliente.setCorreo((String) body.get("correo"));
            }

            cliente.setEstado((short) 1);
            return cliente;

        } catch (Exception e) {
            // Si ApiCloud falla (cliente no encontrado, timeout, etc.)
            return null;
        }
    }
}


