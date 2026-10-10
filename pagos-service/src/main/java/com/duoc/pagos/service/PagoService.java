package com.duoc.pagos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duoc.pagos.dto.DepositoRequest;
import com.duoc.pagos.dto.OperacionPagoResponse;
import com.duoc.pagos.dto.PagoRequest;
import com.duoc.pagos.dto.TransferenciaRequest;
import com.duoc.pagos.model.CuentaSaldo;
import com.duoc.pagos.model.OperacionPago;
import com.duoc.pagos.repository.CuentaSaldoRepository;
import com.duoc.pagos.repository.OperacionPagoRepository;

import com.duoc.pagos.kafka.event.OperacionRealizadaEvent;
import com.duoc.pagos.kafka.producer.OperacionEventProducer;

@Service
public class PagoService {

        private final CuentaSaldoRepository cuentaSaldoRepository;
        private final OperacionPagoRepository operacionPagoRepository;
        private final OperacionEventProducer operacionEventProducer;

        public PagoService(
                CuentaSaldoRepository cuentaSaldoRepository,
                OperacionPagoRepository operacionPagoRepository,
                OperacionEventProducer operacionEventProducer) {

                this.cuentaSaldoRepository = cuentaSaldoRepository;
                this.operacionPagoRepository = operacionPagoRepository;
                this.operacionEventProducer = operacionEventProducer;
        }

        @Transactional
        public OperacionPagoResponse realizarPago(PagoRequest request) {

                validarMonto(request.monto());

                CuentaSaldo cuenta = obtenerCuentaValida(request.cuentaId());

                if (cuenta.saldo().compareTo(request.monto()) < 0) {
                throw new IllegalArgumentException("Saldo insuficiente para realizar el pago");
                }

                int actualizadas = cuentaSaldoRepository.debitar(
                        request.cuentaId(),
                        request.monto());

                if (actualizadas != 1) {
                throw new IllegalStateException("No fue posible debitar la cuenta");
                }

                CuentaSaldo cuentaActualizada = obtenerCuentaValida(request.cuentaId());

                LocalDateTime fechaHora = LocalDateTime.now(ZoneOffset.UTC);
                String operacionId = UUID.randomUUID().toString();

                OperacionPago operacion = new OperacionPago(
                        operacionId,
                        "PAGO",
                        request.cuentaId(),
                        null,
                        request.monto(),
                        "COMPLETADA",
                        request.descripcion(),
                        fechaHora);

                operacionPagoRepository.guardar(operacion);

                operacionEventProducer.publicar(
                        new OperacionRealizadaEvent(
                                operacionId,
                                "PAGO",
                                request.cuentaId(),
                                null,
                                request.monto(),
                                cuentaActualizada.saldo(),
                                null,
                                fechaHora));

                return new OperacionPagoResponse(
                        operacionId,
                        "PAGO",
                        request.cuentaId(),
                        null,
                        request.monto(),
                        cuentaActualizada.saldo(),
                        null,
                        "COMPLETADA",
                        "Pago realizado correctamente",
                        fechaHora);
        }

        @Transactional
        public OperacionPagoResponse realizarDeposito(DepositoRequest request) {

                validarMonto(request.monto());

                obtenerCuentaValida(request.cuentaId());

                int actualizadas = cuentaSaldoRepository.acreditar(
                        request.cuentaId(),
                        request.monto());

                if (actualizadas != 1) {
                throw new IllegalStateException("No fue posible acreditar la cuenta");
                }

                CuentaSaldo cuentaActualizada = obtenerCuentaValida(request.cuentaId());

                LocalDateTime fechaHora = LocalDateTime.now(ZoneOffset.UTC);
                String operacionId = UUID.randomUUID().toString();

                OperacionPago operacion = new OperacionPago(
                        operacionId,
                        "DEPOSITO",
                        null,
                        request.cuentaId(),
                        request.monto(),
                        "COMPLETADA",
                        request.descripcion(),
                        fechaHora);

                operacionPagoRepository.guardar(operacion);

                operacionEventProducer.publicar(
                        new OperacionRealizadaEvent(
                                operacionId,
                                "DEPOSITO",
                                null,
                                request.cuentaId(),
                                request.monto(),
                                null,
                                cuentaActualizada.saldo(),
                                fechaHora));

                return new OperacionPagoResponse(
                        operacionId,
                        "DEPOSITO",
                        null,
                        request.cuentaId(),
                        request.monto(),
                        null,
                        cuentaActualizada.saldo(),
                        "COMPLETADA",
                        "Depósito realizado correctamente",
                        fechaHora);
        }

        @Transactional
        public OperacionPagoResponse realizarTransferencia(
                TransferenciaRequest request) {

                validarMonto(request.monto());

                if (request.cuentaOrigenId() == null ||
                        request.cuentaDestinoId() == null) {
                throw new IllegalArgumentException(
                        "Las cuentas de origen y destino son obligatorias");
                }

                if (request.cuentaOrigenId().equals(request.cuentaDestinoId())) {
                throw new IllegalArgumentException(
                        "La cuenta de origen y destino no pueden ser la misma");
                }

                CuentaSaldo origen = obtenerCuentaValida(request.cuentaOrigenId());
                obtenerCuentaValida(request.cuentaDestinoId());

                if (origen.saldo().compareTo(request.monto()) < 0) {
                throw new IllegalArgumentException(
                        "Saldo insuficiente para realizar la transferencia");
                }

                int debitadas = cuentaSaldoRepository.debitar(
                        request.cuentaOrigenId(),
                        request.monto());

                if (debitadas != 1) {
                throw new IllegalStateException(
                        "No fue posible debitar la cuenta de origen");
                }

                int acreditadas = cuentaSaldoRepository.acreditar(
                        request.cuentaDestinoId(),
                        request.monto());

                if (acreditadas != 1) {
                throw new IllegalStateException(
                        "No fue posible acreditar la cuenta de destino");
                }

                CuentaSaldo origenActualizado =
                        obtenerCuentaValida(request.cuentaOrigenId());

                CuentaSaldo destinoActualizado =
                        obtenerCuentaValida(request.cuentaDestinoId());

                LocalDateTime fechaHora = LocalDateTime.now(ZoneOffset.UTC);
                String operacionId = UUID.randomUUID().toString();

                OperacionPago operacion = new OperacionPago(
                        operacionId,
                        "TRANSFERENCIA",
                        request.cuentaOrigenId(),
                        request.cuentaDestinoId(),
                        request.monto(),
                        "COMPLETADA",
                        request.descripcion(),
                        fechaHora);

                operacionPagoRepository.guardar(operacion);

                operacionEventProducer.publicar(
                        new OperacionRealizadaEvent(
                                operacionId,
                                "TRANSFERENCIA",
                                request.cuentaOrigenId(),
                                request.cuentaDestinoId(),
                                request.monto(),
                                origenActualizado.saldo(),
                                destinoActualizado.saldo(),
                                fechaHora));

                return new OperacionPagoResponse(
                        operacionId,
                        "TRANSFERENCIA",
                        request.cuentaOrigenId(),
                        request.cuentaDestinoId(),
                        request.monto(),
                        origenActualizado.saldo(),
                        destinoActualizado.saldo(),
                        "COMPLETADA",
                        "Transferencia realizada correctamente",
                        fechaHora);
        }

        private CuentaSaldo obtenerCuentaValida(Integer cuentaId) {

                if (cuentaId == null) {
                throw new IllegalArgumentException("La cuenta es obligatoria");
                }

                CuentaSaldo cuenta = cuentaSaldoRepository.findById(cuentaId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La cuenta " + cuentaId + " no existe"));

                if (!"VALIDA".equalsIgnoreCase(cuenta.estado())) {
                throw new IllegalArgumentException(
                        "La cuenta " + cuentaId + " no se encuentra válida");
                }

                return cuenta;
        }

        private void validarMonto(BigDecimal monto) {

                if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "El monto debe ser mayor que cero");
                }
        }
}