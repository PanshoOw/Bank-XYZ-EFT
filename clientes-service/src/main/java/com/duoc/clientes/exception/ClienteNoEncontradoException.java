package com.duoc.clientes.exception;

public class ClienteNoEncontradoException extends RuntimeException {

    public ClienteNoEncontradoException(Long clienteId) {
        super("Cliente no encontrado: " + clienteId);
    }
}