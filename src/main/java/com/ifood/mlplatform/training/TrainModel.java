package com.ifood.mlplatform.training;

import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.IntVector;
import smile.classification.RandomForest;
import smile.io.Read;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


import org.apache.commons.csv.CSVFormat;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TrainModel {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            log.info("Usage: java -jar mini-ml-platform-1.0.0-jar-with-dependencies.jar <path-to-csv>");
            System.exit(1);
        }

        String csvPath = args[0];
        Path path = Path.of(csvPath);
        if (!Files.exists(path)) {
            throw new IllegalStateException("❌ File data/iris.csv not found.");
        }
        log.info("Starting training job...");
        log.info("Reading data from={}", csvPath);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(false)
                .build();

        DataFrame data = Read.csv(csvPath, csvFormat);

        log.info("Columns found: ");
        for (String name : data.names()) {
            log.info(" - " + name);
        }

        String labelColumn = "class";
        String[] species = data.stringVector(labelColumn).distinct().toArray(new String[0]);
        Map<String, Integer> speciesMap = new HashMap<>();
        for (int i = 0; i < species.length; i++) {
            speciesMap.put(species[i], i);
        }

        String[] labelValues = data.column(labelColumn).toStringArray();
        int[] labels = new int[labelValues.length];
        for (int i = 0; i < labelValues.length; i++) {
            labels[i] = speciesMap.get(labelValues[i]);
        }

        data = data.drop(labelColumn).merge(IntVector.of(labelColumn, labels));

        log.info("Label mapping applied={}", speciesMap);

        Formula formula = Formula.lhs(labelColumn);

        log.info("Starting model training with label column={}", labelColumn);

        RandomForest model = RandomForest.fit(formula, data);

        log.info("Model training completed. Saving model...");

        // Serializar modelo
        ByteArrayOutputStream modelBytes = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(modelBytes)) {
            oos.writeObject(model);
        }

        // Serializar schema
        ByteArrayOutputStream schemaBytes = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(schemaBytes)) {
            oos.writeObject(data.schema());
        }

        String bucketName = System.getenv().getOrDefault("BUCKET_NAME", "model");
        String endpoint = System.getenv().getOrDefault("MINIO_ENDPOINT", "http://localhost:9000");
        String accessKey = System.getenv().getOrDefault("MINIO_ACCESS_KEY", "admin");
        String secretKey = System.getenv().getOrDefault("MINIO_SECRET_KEY", "admin123");

        // Cria o cliente S3 apontando para MinIO
        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // Upload do modelo
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object("model.bin")
                .stream(new ByteArrayInputStream(modelBytes.toByteArray()), modelBytes.size(), -1)
                .contentType("application/octet-stream")
                .build()
        );

        // Upload do schema
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object("schema.bin")
                .stream(new ByteArrayInputStream(schemaBytes.toByteArray()), schemaBytes.size(), -1)
                .contentType("application/octet-stream")
                .build()
        );
        log.info("✅ Model and schema uploaded to bucket={}", bucketName);
        
    }
}
