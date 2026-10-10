package com.duoc.bffweb.service;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.duoc.bffweb.dto.ActualizarClienteWebRequest;
import com.duoc.bffweb.dto.ClienteWebResponse;
import com.duoc.bffweb.dto.CrearClienteWebRequest;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

@Service
public class ClienteWebService {

    private static final Logger log =
            LoggerFactory.getLogger(ClienteWebService.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Bulkhead bulkhead;

    public ClienteWebService(
            @Value("${bankxyz.clientes.url}") String clientesUrl,
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory,
            RetryRegistry retryRegistry,
            BulkheadRegistry bulkheadRegistry) {

        this.restClient = loadBalancedRestClientBuilder
                .baseUrl(clientesUrl)
                .build();

        this.circuitBreaker =
                circuitBreakerFactory.create("circuitBreaker");

        this.retry =
                retryRegistry.retry("backendRead");

        this.bulkhead =
                bulkheadRegistry.bulkhead("backendRead");
    }

    public List<ClienteWebResponse> obtenerClientes() {

        String token = obtenerTokenActual();

        Supplier<List<ClienteWebResponse>> consulta = () -> {

            ClienteWebResponse[] clientes = restClient.get()
                    .uri("/api/clientes")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + token)
                    .retrieve()
                    .body(ClienteWebResponse[].class);

            if (clientes == null) {
                return List.of();
            }

            return Arrays.asList(clientes);
        };

        Supplier<List<ClienteWebResponse>> conBulkhead =
                Bulkhead.decorateSupplier(bulkhead, consulta);

        Supplier<List<ClienteWebResponse>> conRetry =
                Retry.decorateSupplier(retry, conBulkhead);

        return circuitBreaker.run(
                conRetry,
                this::obtenerClientesFallback);
    }

    public ClienteWebResponse obtenerCliente(Long id) {

        String token = obtenerTokenActual();

        Supplier<ClienteWebResponse> consulta = () ->
                restClient.get()
                        .uri("/api/clientes/{id}", id)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token)
                        .retrieve()
                        .body(ClienteWebResponse.class);

        Supplier<ClienteWebResponse> conBulkhead =
                Bulkhead.decorateSupplier(bulkhead, consulta);

        Supplier<ClienteWebResponse> conRetry =
                Retry.decorateSupplier(retry, conBulkhead);

        return circuitBreaker.run(
                conRetry,
                throwable -> obtenerClienteFallback(id, throwable));
    }

    public ClienteWebResponse crearCliente(
            CrearClienteWebRequest request) {

        String token = obtenerTokenActual();

        return restClient.post()
                .uri("/api/clientes")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token)
                .body(request)
                .retrieve()
                .body(ClienteWebResponse.class);
    }

    public ClienteWebResponse actualizarCliente(
            Long id,
            ActualizarClienteWebRequest request) {

        String token = obtenerTokenActual();

        return restClient.put()
                .uri("/api/clientes/{id}", id)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token)
                .body(request)
                .retrieve()
                .body(ClienteWebResponse.class);
    }

    private List<ClienteWebResponse> obtenerClientesFallback(
            Throwable throwable) {

        log.warn(
                "Fallback Web al consultar clientes",
                throwable);

        return List.of();
    }

    private ClienteWebResponse obtenerClienteFallback(
            Long id,
            Throwable throwable) {

        log.warn(
                "Fallback Web al consultar el cliente {}",
                id,
                throwable);

        return new ClienteWebResponse(
                id,
                null,
                "Servicio no disponible",
                null,
                "N/A",
                "TEMPORALMENTE_NO_DISPONIBLE",
                null,
                null);
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