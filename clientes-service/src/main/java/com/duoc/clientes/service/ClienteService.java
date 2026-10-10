package com.duoc.clientes.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duoc.clientes.dto.ActualizarClienteRequest;
import com.duoc.clientes.dto.CrearClienteRequest;
import com.duoc.clientes.model.Cliente;
import com.duoc.clientes.repository.ClienteRepository;

import com.duoc.clientes.exception.ClienteNoEncontradoException;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    public Cliente obtenerPorId(Long clienteId) {

        return clienteRepository.findById(clienteId)
                .orElseThrow(() ->
                        new ClienteNoEncontradoException(clienteId)
                );
    }

    @Transactional
    public Cliente crear(CrearClienteRequest request) {

        validarCuentaId(request.cuentaId());
        validarNombre(request.nombre());
        validarEdad(request.edad());

        String perfil = normalizarPerfil(request.perfil());
        String estado = normalizarEstado(request.estado());

        if (clienteRepository.existsByCuentaId(request.cuentaId())) {
            throw new IllegalStateException(
                    "Ya existe un cliente asociado a la cuenta "
                            + request.cuentaId()
            );
        }

        clienteRepository.insertar(
                request.cuentaId(),
                request.nombre().trim(),
                request.edad(),
                perfil,
                estado
        );

        return clienteRepository.findByCuentaId(request.cuentaId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No fue posible recuperar el cliente creado"
                        )
                );
    }

    @Transactional
    public Cliente actualizar(
            Long clienteId,
            ActualizarClienteRequest request) {

        obtenerPorId(clienteId);

        validarNombre(request.nombre());
        validarEdad(request.edad());

        String perfil = normalizarPerfil(request.perfil());
        String estado = normalizarEstado(request.estado());

        int filas = clienteRepository.actualizar(
                clienteId,
                request.nombre().trim(),
                request.edad(),
                perfil,
                estado
        );

        if (filas != 1) {
            throw new IllegalStateException(
                    "No fue posible actualizar el cliente "
                            + clienteId
            );
        }

        return obtenerPorId(clienteId);
    }

    private void validarCuentaId(Integer cuentaId) {
        if (cuentaId == null || cuentaId <= 0) {
            throw new IllegalArgumentException(
                    "cuentaId debe ser mayor que cero"
            );
        }
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio"
            );
        }

        if (nombre.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar 120 caracteres"
            );
        }
    }

    private void validarEdad(Integer edad) {
        if (edad == null || edad < 0 || edad > 120) {
            throw new IllegalArgumentException(
                    "La edad debe estar entre 0 y 120 años"
            );
        }
    }

    private String normalizarPerfil(String perfil) {

        if (perfil == null || perfil.isBlank()) {
            return "ESTANDAR";
        }

        String valor = perfil.trim().toUpperCase();

        if (!valor.equals("ESTANDAR")
                && !valor.equals("PREFERENTE")) {

            throw new IllegalArgumentException(
                    "Perfil inválido. Valores permitidos: "
                            + "ESTANDAR, PREFERENTE"
            );
        }

        return valor;
    }

    private String normalizarEstado(String estado) {

        if (estado == null || estado.isBlank()) {
            return "ACTIVO";
        }

        String valor = estado.trim().toUpperCase();

        if (!valor.equals("ACTIVO")
                && !valor.equals("INACTIVO")) {

            throw new IllegalArgumentException(
                    "Estado inválido. Valores permitidos: "
                            + "ACTIVO, INACTIVO"
            );
        }

        return valor;
    }
}