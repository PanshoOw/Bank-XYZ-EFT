package com.duoc.bffmobile.exception;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class DownstreamExceptionHandler {

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> manejarErrorDeMicroservicio(
            RestClientResponseException exception) {

        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(exception.getResponseBodyAsString());
    }
}