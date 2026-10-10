package com.duoc.clientes.controller;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.duoc.clientes.dto.ErrorResponse;

import com.duoc.clientes.exception.ClienteNoEncontradoException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleBadRequest(
                IllegalArgumentException ex) {

                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(
                                400,
                                "BAD_REQUEST",
                                ex.getMessage(),
                                LocalDateTime.now(ZoneOffset.UTC)
                        ));
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ErrorResponse> handleConflict(
                IllegalStateException ex) {

                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse(
                                409,
                                "CONFLICT",
                                ex.getMessage(),
                                LocalDateTime.now(ZoneOffset.UTC)
                        ));
        }

        @ExceptionHandler(ClienteNoEncontradoException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(
                ClienteNoEncontradoException ex) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(
                                404,
                                "NOT_FOUND",
                                ex.getMessage(),
                                LocalDateTime.now(ZoneOffset.UTC)
                        ));
        }
}