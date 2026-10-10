package com.duoc.bffatm.service;

import java.math.BigDecimal;

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

import com.duoc.bffatm.dto.CuentaBackendResponse;
import com.duoc.bffatm.dto.RetiroAtmRequest;
import com.duoc.bffatm.dto.RetiroAtmResponse;
import com.duoc.bffatm.dto.RetiroBackendResponse;
import com.duoc.bffatm.dto.SaldoAtmResponse;

@Service
public class CuentaAtmService {

        private static final Logger log =
                LoggerFactory.getLogger(CuentaAtmService.class);

        private final RestClient restClient;
        private final CircuitBreaker circuitBreaker;

        public CuentaAtmService(
                @Value("${bankxyz.cuentas.url}") String cuentasUrl,
                @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
                CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

                this.restClient = loadBalancedRestClientBuilder
                        .baseUrl(cuentasUrl)
                        .build();

                this.circuitBreaker =
                        circuitBreakerFactory.create("circuitBreaker");
        }

        public SaldoAtmResponse obtenerSaldo(Integer id) {

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

                        return new SaldoAtmResponse(
                                cuenta.cuentaId(),
                                cuenta.saldoFinal(),
                                cuenta.estado());
                        },
                        throwable ->
                                obtenerSaldoFallback(id, throwable));
        }

        private SaldoAtmResponse obtenerSaldoFallback(
                Integer id,
                Throwable throwable) {

                log.warn(
                        "Fallback ATM al consultar saldo de la cuenta {}",
                        id,
                        throwable);

                return new SaldoAtmResponse(
                        id,
                        BigDecimal.ZERO,
                        "TEMPORALMENTE_NO_DISPONIBLE");
        }

        public RetiroAtmResponse retirar(
                Integer id,
                RetiroAtmRequest request) {

                String token = obtenerTokenActual();

                return circuitBreaker.run(
                        () -> {

                        RetiroBackendResponse respuesta =
                                restClient.post()
                                        .uri("/api/cuentas/{id}/retiro", id)
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                "Bearer " + token)
                                        .body(request)
                                        .retrieve()
                                        .body(RetiroBackendResponse.class);

                        return new RetiroAtmResponse(
                                respuesta.cuentaId(),
                                respuesta.montoRetirado(),
                                respuesta.saldoDisponible(),
                                "CONFIRMADA",
                                respuesta.mensaje());
                        },
                        throwable ->
                                retirarFallback(id, throwable));
        }

        private RetiroAtmResponse retirarFallback(
                Integer id,
                Throwable throwable) {

                log.warn(
                        "Fallback ATM al retirar de la cuenta {}",
                        id,
                        throwable);

                return new RetiroAtmResponse(
                        id,
                        null,
                        null,
                        "NO_VERIFICADA",
                        "No fue posible confirmar el resultado del retiro. Consulte el saldo antes de reintentar.");
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
}