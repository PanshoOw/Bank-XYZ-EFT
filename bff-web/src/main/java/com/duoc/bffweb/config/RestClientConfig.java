package com.duoc.bffweb.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "restClientBuilder")
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {

        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {

                    Authentication authentication =
                            SecurityContextHolder
                                    .getContext()
                                    .getAuthentication();

                    if (authentication
                            instanceof JwtAuthenticationToken jwtAuth) {

                        request.getHeaders().setBearerAuth(
                                jwtAuth.getToken().getTokenValue()
                        );
                    }

                    return execution.execute(request, body);
                });
    }
}