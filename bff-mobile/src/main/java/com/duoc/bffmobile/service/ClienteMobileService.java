package com.duoc.bffmobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.duoc.bffmobile.dto.ClienteBackendResponse;
import com.duoc.bffmobile.dto.ClienteMobileResponse;

@Service
public class ClienteMobileService {

    private static final Logger log =
            LoggerFactory.getLogger(ClienteMobileService.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public ClienteMobileService(
            @Value("${bankxyz.clientes.url}") String clientesUrl,
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.restClient = loadBalancedRestClientBuilder
                .baseUrl(clientesUrl)
                .build();

        this.circuitBreaker =
                circuitBreakerFactory.create("circuitBreaker");
    }

    public ClienteMobileResponse obtenerCliente(Long id) {

        String token = obtenerTokenActual();

        return circuitBreaker.run(
                () -> {

                    ClienteBackendResponse cliente =
                            restClient.get()
                                    .uri("/api/clientes/{id}", id)
                                    .headers(headers ->
                                            headers.setBearerAuth(token))
                                    .retrieve()
                                    .body(ClienteBackendResponse.class);

                    return convertirAMobile(cliente);
                },
                throwable ->
                        obtenerClienteFallback(id, throwable));
    }

    private ClienteMobileResponse obtenerClienteFallback(
            Long id,
            Throwable throwable) {

        log.warn(
                "Fallback Mobile al consultar el cliente {}",
                id,
                throwable);

        return new ClienteMobileResponse(
                id,
                null,
                "Servicio no disponible",
                "N/A",
                "TEMPORALMENTE_NO_DISPONIBLE");
    }

    private ClienteMobileResponse convertirAMobile(
            ClienteBackendResponse cliente) {

        return new ClienteMobileResponse(
                cliente.clienteId(),
                cliente.cuentaId(),
                cliente.nombre(),
                cliente.perfil(),
                cliente.estado());
    }

    private String obtenerTokenActual() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication
                instanceof JwtAuthenticationToken jwtAuthentication) {

            return jwtAuthentication
                    .getToken()
                    .getTokenValue();
        }

        throw new IllegalStateException(
                "No existe un JWT autenticado para reenviar a clientes-service");
    }
}