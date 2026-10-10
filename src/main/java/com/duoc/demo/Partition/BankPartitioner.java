package com.duoc.demo.Partition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.core.io.Resource;

public class BankPartitioner implements Partitioner {

    private static final Logger log =
            LoggerFactory.getLogger(BankPartitioner.class);

    private final Resource inputResource;

    public BankPartitioner(Resource inputResource) {
        this.inputResource = inputResource;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        if (gridSize <= 0) {
            throw new IllegalArgumentException(
                    "gridSize debe ser mayor que cero");
        }

        int totalRecords = countDataRecords();

        Map<String, ExecutionContext> partitions =
                new LinkedHashMap<>();

        log.info("Grid size: {}", gridSize);
        log.info("Total records: {}", totalRecords);

        if (totalRecords == 0) {
            return partitions;
        }

        int partitionSize =
                (int) Math.ceil(
                        (double) totalRecords / gridSize);

        for (int i = 0; i < gridSize; i++) {

            int start = i * partitionSize;

            if (start >= totalRecords) {
                break;
            }

            int end = Math.min(
                    start + partitionSize,
                    totalRecords);

            ExecutionContext context =
                    new ExecutionContext();

            context.putInt("start", start);
            context.putInt("end", end);

            partitions.put(
                    "partition" + i,
                    context);
        }

        return partitions;
    }

    private int countDataRecords() {

        long totalRecords;

        try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputResource.getInputStream(),
                                    StandardCharsets.UTF_8))) {

            String header = reader.readLine();

            if (header == null) {
                return 0;
            }

            totalRecords = reader.lines().count();

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No fue posible contar los registros del archivo: "
                            + inputResource.getDescription(),
                    ex);
        }

        if (totalRecords > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "El archivo contiene demasiados registros para "
                            + "ser particionado mediante índices int");
        }

        return (int) totalRecords;
    }
}