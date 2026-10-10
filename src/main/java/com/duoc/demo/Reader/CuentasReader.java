package com.duoc.demo.Reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.core.io.ClassPathResource;

import com.duoc.demo.Model.LegacyCuentas;

public class CuentasReader
        extends AbstractItemCountingItemStreamItemReader<LegacyCuentas> {

    private static final int CANTIDAD_COLUMNAS_ESPERADAS = 5;

    private final String resourcePath;
    private final int start;

    private BufferedReader reader;
    private int numeroLineaFisica;

    public CuentasReader(
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

        setName("cuentasReader");
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

        String header = reader.readLine();

        if (header == null) {
            throw new IOException(
                    "El archivo CSV esta vacio: " + resourcePath);
        }

        numeroLineaFisica = 1;

        /*
         * Posiciona el Reader al comienzo
         * correspondiente a esta particion.
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
    protected LegacyCuentas doRead() throws Exception {

        String line = reader.readLine();

        if (line == null) {
            return null;
        }

        numeroLineaFisica++;

        /*
         * El -1 preserva, por ejemplo, una
         * descripcion vacia al final del CSV.
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

        return new LegacyCuentas(
        fields[0],
        fields[1],
        fields[2],
        fields[3],
        fields[4],
        numeroLineaFisica);
    }

    @Override
    protected void jumpToItem(int itemIndex) throws Exception {

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
