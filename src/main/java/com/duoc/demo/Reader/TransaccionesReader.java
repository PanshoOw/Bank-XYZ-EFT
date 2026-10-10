package com.duoc.demo.Reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.core.io.ClassPathResource;

import com.duoc.demo.Model.LegacyTransacciones;

public class TransaccionesReader
        extends AbstractItemCountingItemStreamItemReader<LegacyTransacciones> {

    private static final int CANTIDAD_COLUMNAS_ESPERADAS = 4;

    private final String resourcePath;
    private final int start;

    private BufferedReader reader;
    private int numeroLineaFisica;

    public TransaccionesReader(
            String resourcePath,
            int start,
            int end) {

        if (start < 0) {
            throw new IllegalArgumentException(
                    "El indice inicial no puede ser negativo");
        }

        if (end < start) {
            throw new IllegalArgumentException(
                    "El indice final no puede ser menor que el inicial");
        }

        this.resourcePath = resourcePath;
        this.start = start;

        /*
         * Nombre utilizado por Spring Batch para guardar
         * el estado del Reader en el ExecutionContext.
         */
        setName("transaccionesReader");

        /*
         * Cada particion solo procesa la cantidad de
         * registros comprendida entre start y end.
         */
        setMaxItemCount(end - start);
    }

    @Override
    protected void doOpen() throws Exception {

        ClassPathResource resource =
                new ClassPathResource(resourcePath);

        reader = new BufferedReader(
                new InputStreamReader(
                        resource.getInputStream(),
                        StandardCharsets.UTF_8));

        /*
         * La primera linea corresponde al encabezado CSV.
         */
        String header = reader.readLine();

        if (header == null) {
            throw new IOException(
                    "El archivo CSV esta vacio: " + resourcePath);
        }

        numeroLineaFisica = 1;

        /*
         * Nos posicionamos directamente al inicio
         * correspondiente a esta particion.
         *
         * No cargamos el archivo completo en memoria.
         */
        for (int i = 0; i < start; i++) {

            String linea = reader.readLine();

            if (linea == null) {
                break;
            }

            numeroLineaFisica++;
        }
    }

    @Override
    protected LegacyTransacciones doRead() throws Exception {

        String line = reader.readLine();

        if (line == null) {
            return null;
        }

        numeroLineaFisica++;

        /*
         * El -1 conserva columnas vacias al final.
         *
         * Ejemplo:
         * 1,2024-01-01,1000,
         *
         * Sin -1 Java eliminaria la ultima columna vacia.
         */
        String[] fields = line.split(",", -1);

        if (fields.length != CANTIDAD_COLUMNAS_ESPERADAS) {

            throw new FlatFileParseException(
                    "Cantidad de columnas invalida. Se esperaban "
                            + CANTIDAD_COLUMNAS_ESPERADAS
                            + " pero se encontraron "
                            + fields.length,
                    line,
                    numeroLineaFisica);
        }

        return new LegacyTransacciones(
                fields[0],
                fields[1],
                fields[2],
                fields[3]);
    }

    @Override
    protected void jumpToItem(int itemIndex) throws Exception {

        /*
         * Durante un restart, Spring Batch recupera el contador
         * desde el ExecutionContext.
         *
         * Como doOpen() ya dejo el Reader al comienzo de la
         * particion, solamente avanzamos los registros que
         * esa particion ya habia procesado.
         */
        for (int i = 0; i < itemIndex; i++) {

            String line = reader.readLine();

            if (line == null) {
                return;
            }

            numeroLineaFisica++;
        }
    }

    @Override
    protected void doClose() throws Exception {

        if (reader != null) {
            reader.close();
        }
    }
}