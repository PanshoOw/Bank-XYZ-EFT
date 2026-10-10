package com.duoc.bffmobile.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.duoc.bffmobile.dto.OperacionMobileResponse;
import com.duoc.bffmobile.dto.OperacionPagoBackendResponse;
import com.duoc.bffmobile.dto.PagoMobileRequest;
import com.duoc.bffmobile.dto.TransferenciaMobileRequest;

@Service
public class PagoMobileService {

    private final RestClient restClient;

    public PagoMobileService(
            @Value("${bankxyz.pagos.url}") String pagosUrl,
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {

        this.restClient = loadBalancedRestClientBuilder
                .baseUrl(pagosUrl)
                .build();
    }

    public OperacionMobileResponse realizarPago(
            PagoMobileRequest request) {

        String token = obtenerTokenActual();

        OperacionPagoBackendResponse respuesta =
                restClient.post()
                        .uri("/api/pagos")
                        .headers(headers ->
                                headers.setBearerAuth(token))
                        .body(request)
                        .retrieve()
                        .body(OperacionPagoBackendResponse.class);

        return convertirAMobile(respuesta);
    }

    public OperacionMobileResponse realizarTransferencia(
            TransferenciaMobileRequest request) {

        String token = obtenerTokenActual();

        OperacionPagoBackendResponse respuesta =
                restClient.post()
                        .uri("/api/transferencias")
                        .headers(headers ->
                                headers.setBearerAuth(token))
                        .body(request)
                        .retrieve()
                        .body(OperacionPagoBackendResponse.class);

        return convertirAMobile(respuesta);
    }

    private OperacionMobileResponse convertirAMobile(
            OperacionPagoBackendResponse respuesta) {

        return new OperacionMobileResponse(
                respuesta.operacionId(),
                respuesta.tipo(),
                respuesta.monto(),
                respuesta.saldoOrigen(),
                respuesta.saldoDestino(),
                respuesta.estado(),
                respuesta.mensaje());
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