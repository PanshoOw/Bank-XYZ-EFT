package com.duoc.bffmobile.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

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

import com.duoc.bffmobile.dto.CuentaBackendResponse;
import com.duoc.bffmobile.dto.CuentaMobileResponse;

@Service
public class CuentaMobileService {

        private static final Logger log =
                LoggerFactory.getLogger(CuentaMobileService.class);

        private final RestClient restClient;
        private final CircuitBreaker circuitBreaker;

        public CuentaMobileService(
                @Value("${bankxyz.cuentas.url}") String cuentasUrl,
                @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
                CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

                this.restClient = loadBalancedRestClientBuilder
                        .baseUrl(cuentasUrl)
                        .build();

                this.circuitBreaker =
                        circuitBreakerFactory.create("circuitBreaker");
        }

        public List<CuentaMobileResponse> obtenerCuentas() {

                String token = obtenerTokenActual();

                return circuitBreaker.run(
                        () -> {

                        CuentaBackendResponse[] cuentas =
                                restClient.get()
                                        .uri("/api/cuentas")
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                "Bearer " + token)
                                        .retrieve()
                                        .body(CuentaBackendResponse[].class);

                        if (cuentas == null) {
                                return List.of();
                        }

                        return Arrays.stream(cuentas)
                                .map(this::convertirAMobile)
                                .toList();
                        },
                        this::obtenerCuentasFallback);
        }

        private List<CuentaMobileResponse> obtenerCuentasFallback(
                Throwable throwable) {

                log.warn(
                        "Fallback Mobile al listar cuentas",
                        throwable);

                return List.of();
        }

        public CuentaMobileResponse obtenerCuenta(Integer id) {

                String token = obtenerTokenActual();

                return circuitBreaker.run(
                        () -> {

                        CuentaBackendResponse cuenta =
                                restClient.get()
                                        .uri("/api/cuentas/{id}", id)
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                "Bearer " + token)
                                        .retrieve()
                                        .body(CuentaBackendResponse.class);

                        return convertirAMobile(cuenta);
                        },
                        throwable ->
                                obtenerCuentaFallback(id, throwable));
        }

        private CuentaMobileResponse obtenerCuentaFallback(
                Integer id,
                Throwable throwable) {

                log.warn(
                        "Fallback Mobile al consultar la cuenta {}",
                        id,
                        throwable);

                return new CuentaMobileResponse(
                        id,
                        "Servicio no disponible",
                        "N/A",
                        BigDecimal.ZERO,
                        "TEMPORALMENTE_NO_DISPONIBLE");
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
                        "No existe un JWT autenticado para reenviar a cuentas-service");
        }

        private CuentaMobileResponse convertirAMobile(
                CuentaBackendResponse cuenta) {

                return new CuentaMobileResponse(
                        cuenta.cuentaId(),
                        cuenta.nombre(),
                        cuenta.tipo(),
                        cuenta.saldoFinal(),
                        cuenta.estado());
        }
}