package com.boticavidasalud.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        // Tiempo máximo para establecer la conexión.
        requestFactory.setConnectTimeout(
                Duration.ofSeconds(3)
        );

        // Tiempo máximo de espera para recibir la respuesta.
        requestFactory.setReadTimeout(
                Duration.ofSeconds(5)
        );

        return new RestTemplate(requestFactory);
    }
}


