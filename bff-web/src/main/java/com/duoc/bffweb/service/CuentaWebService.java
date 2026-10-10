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

import com.duoc.bffweb.dto.ActualizarCuentaBackendRequest;
import com.duoc.bffweb.dto.ActualizarCuentaWebRequest;
import com.duoc.bffweb.dto.CrearCuentaBackendRequest;
import com.duoc.bffweb.dto.CrearCuentaWebRequest;
import com.duoc.bffweb.dto.CuentaBackendResponse;
import com.duoc.bffweb.dto.CuentaWebResponse;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

@Service
public class CuentaWebService {

        private static final Logger log =
                LoggerFactory.getLogger(CuentaWebService.class);

        private static final String BEARER_PREFIX = "Bearer ";

        private final RestClient restClient;
        private final CircuitBreaker circuitBreaker;
        private final Retry retry;
        private final Bulkhead bulkhead;
        private final String cuentasPath;

        public CuentaWebService(
                @Value("${bankxyz.cuentas.url}") String cuentasUrl,
                @Value("${bankxyz.cuentas.path}") String cuentasPath,
                @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
                CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                RetryRegistry retryRegistry,
                BulkheadRegistry bulkheadRegistry) {

                this.restClient = loadBalancedRestClientBuilder
                        .baseUrl(cuentasUrl)
                        .build();

                this.cuentasPath = cuentasPath;

                this.circuitBreaker =
                        circuitBreakerFactory.create("circuitBreaker");

                this.retry =
                        retryRegistry.retry("backendRead");

                this.bulkhead =
                        bulkheadRegistry.bulkhead("backendRead");
        }

        public CuentaWebResponse obtenerCuenta(Integer id) {

                String token = obtenerTokenActual();

                Supplier<CuentaWebResponse> consulta = () -> {

                CuentaBackendResponse cuenta = restClient.get()
                        .uri(cuentasPath + "/{id}", id)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                BEARER_PREFIX + token
                        )
                        .retrieve()
                        .body(CuentaBackendResponse.class);

                return convertirAWeb(cuenta);
                };

                Supplier<CuentaWebResponse> conBulkhead =
                        Bulkhead.decorateSupplier(bulkhead, consulta);

                Supplier<CuentaWebResponse> conRetry =
                        Retry.decorateSupplier(retry, conBulkhead);

                return circuitBreaker.run(
                        conRetry,
                        throwable -> obtenerCuentaFallback(id, throwable)
                );
        }

        public CuentaWebResponse obtenerCuentaFallback(
                Integer id,
                Throwable e) {

                log.warn(
                        "Fallback Web al consultar la cuenta {}: {}",
                        id,
                        e.getMessage()
                );

                return new CuentaWebResponse(
                        id,
                        "Servicio no disponible",
                        0,
                        "N/A",
                        null,
                        null,
                        null,
                        null,
                        "TEMPORALMENTE_NO_DISPONIBLE"
                );
        }

        public List<CuentaWebResponse> obtenerCuentas() {

                String token = obtenerTokenActual();

                Supplier<List<CuentaWebResponse>> consulta = () -> {

                CuentaBackendResponse[] cuentas = restClient.get()
                        .uri(cuentasPath)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                BEARER_PREFIX + token
                        )
                        .retrieve()
                        .body(CuentaBackendResponse[].class);

                if (cuentas == null) {
                        return List.of();
                }

                return Arrays.stream(cuentas)
                        .map(this::convertirAWeb)
                        .toList();
                };

                Supplier<List<CuentaWebResponse>> conBulkhead =
                        Bulkhead.decorateSupplier(bulkhead, consulta);

                Supplier<List<CuentaWebResponse>> conRetry =
                        Retry.decorateSupplier(retry, conBulkhead);

                return circuitBreaker.run(
                        conRetry,
                        this::obtenerCuentasFallback
                );
        }

        public List<CuentaWebResponse> obtenerCuentasFallback(
                Throwable e) {

                log.warn(
                        "Fallback Web al consultar cuentas: {}",
                        e.getMessage()
                );

                return List.of();
        }

        public CuentaWebResponse abrirCuenta(
                CrearCuentaWebRequest request) {

                String token = obtenerTokenActual();

                CrearCuentaBackendRequest backendRequest =
                        new CrearCuentaBackendRequest(
                                request.cuentaId(),
                                request.nombreCliente(),
                                request.saldoInicial(),
                                request.edadCliente(),
                                request.tipoCuenta(),
                                request.tasaInteres()
                        );

                CuentaBackendResponse cuenta = restClient.post()
                        .uri(cuentasPath)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                BEARER_PREFIX + token
                        )
                        .body(backendRequest)
                        .retrieve()
                        .body(CuentaBackendResponse.class);

                return convertirAWeb(cuenta);
        }

        public CuentaWebResponse actualizarCuenta(
                Integer id,
                ActualizarCuentaWebRequest request) {

                String token = obtenerTokenActual();

                ActualizarCuentaBackendRequest backendRequest =
                        new ActualizarCuentaBackendRequest(
                                request.nombreCliente(),
                                request.edadCliente(),
                                request.tipoCuenta(),
                                request.tasaInteres()
                        );

                CuentaBackendResponse cuenta = restClient.put()
                        .uri(cuentasPath + "/{id}", id)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                BEARER_PREFIX + token
                        )
                        .body(backendRequest)
                        .retrieve()
                        .body(CuentaBackendResponse.class);

                return convertirAWeb(cuenta);
        }

        public CuentaWebResponse cerrarCuenta(Integer id) {

                String token = obtenerTokenActual();

                CuentaBackendResponse cuenta = restClient.patch()
                        .uri(cuentasPath + "/{id}/cerrar", id)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                BEARER_PREFIX + token
                        )
                        .retrieve()
                        .body(CuentaBackendResponse.class);

                return convertirAWeb(cuenta);
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
                        "No existe un JWT autenticado para reenviar a cuentas-service"
                );
        }

        private CuentaWebResponse convertirAWeb(
                CuentaBackendResponse cuenta) {

                if (cuenta == null) {
                throw new IllegalStateException(
                        "cuentas-service no devolvió una respuesta"
                );
                }

                return new CuentaWebResponse(
                        cuenta.cuentaId(),
                        cuenta.nombre(),
                        cuenta.edad(),
                        cuenta.tipo(),
                        cuenta.saldoInicial(),
                        cuenta.tasaInteres(),
                        cuenta.interesCalculado(),
                        cuenta.saldoFinal(),
                        cuenta.estado()
                );
        }
}