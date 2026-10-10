package com.duoc.cuentas.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duoc.cuentas.dto.ActualizarCuentaRequest;
import com.duoc.cuentas.dto.CrearCuentaRequest;
import com.duoc.cuentas.dto.CuentaResponse;
import com.duoc.cuentas.dto.RetiroResponse;
import com.duoc.cuentas.kafka.event.RetiroRealizadoEvent;
import com.duoc.cuentas.kafka.producer.RetiroEventProducer;
import com.duoc.cuentas.repository.CuentaRepository;

@Service
public class CuentaService {

        private static final String ESTADO_VALIDA = "VALIDA";
        private static final String ESTADO_CERRADA = "CERRADA";

        private final CuentaRepository cuentaRepository;
        private final RetiroEventProducer retiroEventProducer;

        public CuentaService(
                CuentaRepository cuentaRepository,
                RetiroEventProducer retiroEventProducer) {

                this.cuentaRepository = cuentaRepository;
                this.retiroEventProducer = retiroEventProducer;
        }

        public List<CuentaResponse> obtenerTodas() {
                return cuentaRepository.findAll();
        }

        public Optional<CuentaResponse> obtenerPorId(Integer cuentaId) {
                return cuentaRepository.findById(cuentaId);
        }

        @Transactional
        public CuentaResponse abrir(CrearCuentaRequest request) {

                validarApertura(request);

                if (cuentaRepository.findById(request.cuentaId()).isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ya existe una cuenta con el identificador indicado"
                );
                }

                String tipo = normalizarTipo(request.tipo());

                int filasInsertadas = cuentaRepository.crear(
                        request.cuentaId(),
                        request.nombre().trim(),
                        request.saldoInicial(),
                        request.edad(),
                        tipo,
                        request.tasaInteres()
                );

                if (filasInsertadas != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No fue posible abrir la cuenta"
                );
                }

                return cuentaRepository.findById(request.cuentaId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "La cuenta fue creada pero no pudo recuperarse"
                                )
                        );
        }

        @Transactional
        public CuentaResponse actualizar(
                Integer cuentaId,
                ActualizarCuentaRequest request) {

                CuentaResponse cuenta = obtenerCuentaObligatoria(cuentaId);

                if (ESTADO_CERRADA.equalsIgnoreCase(cuenta.estado())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No es posible modificar una cuenta cerrada"
                );
                }

                validarMantenimiento(request);

                int filasActualizadas = cuentaRepository.actualizar(
                        cuentaId,
                        request.nombre().trim(),
                        request.edad(),
                        normalizarTipo(request.tipo()),
                        request.tasaInteres()
                );

                if (filasActualizadas != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No fue posible actualizar la cuenta"
                );
                }

                return obtenerCuentaObligatoria(cuentaId);
        }

        @Transactional
        public CuentaResponse cerrar(Integer cuentaId) {

                CuentaResponse cuenta = obtenerCuentaObligatoria(cuentaId);

                if (ESTADO_CERRADA.equalsIgnoreCase(cuenta.estado())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La cuenta ya se encuentra cerrada"
                );
                }

                if (cuenta.saldoFinal().compareTo(BigDecimal.ZERO) != 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La cuenta debe tener saldo cero antes de ser cerrada"
                );
                }

                int filasActualizadas =
                        cuentaRepository.cerrar(cuentaId);

                if (filasActualizadas != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No fue posible cerrar la cuenta"
                );
                }

                return obtenerCuentaObligatoria(cuentaId);
        }

        @Transactional
        public RetiroResponse retirar(
                Integer cuentaId,
                BigDecimal monto) {

                if (monto == null ||
                        monto.compareTo(BigDecimal.ZERO) <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El monto debe ser mayor que cero"
                );
                }

                CuentaResponse cuenta =
                        obtenerCuentaObligatoria(cuentaId);

                if (!ESTADO_VALIDA.equalsIgnoreCase(cuenta.estado())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La cuenta no se encuentra habilitada"
                );
                }

                if (!"ahorro".equalsIgnoreCase(cuenta.tipo())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La cuenta no admite retiros"
                );
                }

                if (cuenta.saldoFinal().compareTo(monto) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Saldo insuficiente"
                );
                }

                int filasActualizadas =
                        cuentaRepository.retirar(cuentaId, monto);

                if (filasActualizadas == 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No fue posible realizar el retiro"
                );
                }

                CuentaResponse cuentaActualizada =
                        obtenerCuentaObligatoria(cuentaId);

                RetiroRealizadoEvent evento =
                        new RetiroRealizadoEvent(
                                cuentaId,
                                monto,
                                cuentaActualizada.saldoFinal(),
                                LocalDateTime.now(ZoneOffset.UTC)
                        );

                retiroEventProducer.publicar(evento);

                return new RetiroResponse(
                        cuentaId,
                        monto,
                        cuentaActualizada.saldoFinal(),
                        "Retiro realizado correctamente"
                );
        }

        private CuentaResponse obtenerCuentaObligatoria(Integer cuentaId) {
                return cuentaRepository.findById(cuentaId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Cuenta no encontrada"
                                )
                        );
        }

        private void validarApertura(CrearCuentaRequest request) {

                if (request == null) {
                throw peticionInvalida();
                }

                if (request.cuentaId() == null || request.cuentaId() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El identificador de cuenta debe ser mayor que cero"
                );
                }

                validarDatosComunes(
                        request.nombre(),
                        request.edad(),
                        request.tipo(),
                        request.tasaInteres()
                );

                if (request.saldoInicial() == null ||
                        request.saldoInicial().compareTo(BigDecimal.ZERO) < 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El saldo inicial no puede ser negativo"
                );
                }
        }

        private void validarMantenimiento(ActualizarCuentaRequest request) {

                if (request == null) {
                throw peticionInvalida();
                }

                validarDatosComunes(
                        request.nombre(),
                        request.edad(),
                        request.tipo(),
                        request.tasaInteres()
                );
        }

        private void validarDatosComunes(
                String nombre,
                Integer edad,
                String tipo,
                BigDecimal tasaInteres) {

                if (nombre == null || nombre.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El nombre es obligatorio"
                );
                }

                if (edad == null || edad <= 0 || edad > 120) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La edad debe estar entre 1 y 120 años"
                );
                }

                normalizarTipo(tipo);

                if (tasaInteres == null ||
                        tasaInteres.compareTo(BigDecimal.ZERO) < 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La tasa de interés no puede ser negativa"
                );
                }
        }

        private String normalizarTipo(String tipo) {

                if (tipo == null || tipo.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El tipo de cuenta es obligatorio"
                );
                }

                String tipoNormalizado =
                        tipo.trim().toLowerCase(Locale.ROOT);

                if (!"ahorro".equals(tipoNormalizado) &&
                        !"prestamo".equals(tipoNormalizado)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El tipo debe ser ahorro o prestamo"
                );
                }

                return tipoNormalizado;
        }

        private ResponseStatusException peticionInvalida() {
                return new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La solicitud no puede estar vacía"
                );
        }
}