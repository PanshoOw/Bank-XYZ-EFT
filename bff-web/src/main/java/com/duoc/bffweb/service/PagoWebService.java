package com.duoc.bffweb.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.duoc.bffweb.dto.DepositoWebRequest;
import com.duoc.bffweb.dto.OperacionPagoWebResponse;
import com.duoc.bffweb.dto.PagoWebRequest;
import com.duoc.bffweb.dto.TransferenciaWebRequest;

@Service
public class PagoWebService {

    private final RestClient restClient;

    public PagoWebService(
            @Value("${bankxyz.pagos.url}") String pagosUrl,
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {

        this.restClient = loadBalancedRestClientBuilder
                .baseUrl(pagosUrl)
                .build();
    }

    public OperacionPagoWebResponse realizarPago(
            PagoWebRequest request) {

        String token = obtenerTokenActual();

        return restClient.post()
                .uri("/api/pagos")
                .headers(headers ->
                        headers.setBearerAuth(token))
                .body(request)
                .retrieve()
                .body(OperacionPagoWebResponse.class);
    }

    public OperacionPagoWebResponse realizarDeposito(
            DepositoWebRequest request) {

        String token = obtenerTokenActual();

        return restClient.post()
                .uri("/api/depositos")
                .headers(headers ->
                        headers.setBearerAuth(token))
                .body(request)
                .retrieve()
                .body(OperacionPagoWebResponse.class);
    }

    public OperacionPagoWebResponse realizarTransferencia(
            TransferenciaWebRequest request) {

        String token = obtenerTokenActual();

        return restClient.post()
                .uri("/api/transferencias")
                .headers(headers ->
                        headers.setBearerAuth(token))
                .body(request)
                .retrieve()
                .body(OperacionPagoWebResponse.class);
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
                "No existe un JWT autenticado para reenviar a pagos-service");
    }
}