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
public class MiApiCloudClient {

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
            ParameterizedTypeReference<Map<String, Object>> responseType =
                    new ParameterizedTypeReference<Map<String, Object>>() {};

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    responseType
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
                return null;
            }

            Object datosObj = body.get("datos");
            if (!(datosObj instanceof Map)) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> datos = (Map<String, Object>) datosObj;

            if (datos == null || datos.isEmpty()) {
                return null;
            }

            ClienteResponseDTO cliente = new ClienteResponseDTO();
            cliente.setTipoDocumento(TipoDocumento.DNI);
            cliente.setNumeroDocumento(dni);

            // 1. Extraer nombres
            String nombres = null;
            if (datos.containsKey("nombres") && datos.get("nombres") != null) {
                nombres = (String) datos.get("nombres");
            }

            if (nombres == null || nombres.isEmpty()) {
                return null;
            }

            // 2. Extraer apellidos (paterno + materno)
            String apellidos = null;
            String paterno = null;
            String materno = null;

            if (datos.containsKey("ape_paterno") && datos.get("ape_paterno") != null) {
                paterno = (String) datos.get("ape_paterno");
            }
            if (datos.containsKey("ape_materno") && datos.get("ape_materno") != null) {
                materno = (String) datos.get("ape_materno");
            }

            if (paterno != null && materno != null) {
                apellidos = paterno + " " + materno;
            } else if (paterno != null) {
                apellidos = paterno;
            } else if (materno != null) {
                apellidos = materno;
            }

            cliente.setNombres(nombres);
            cliente.setApellidos(apellidos);
            cliente.setEstado((short) 1);

            return cliente;

        } catch (Exception e) {
            return null;
        }
    }
}


