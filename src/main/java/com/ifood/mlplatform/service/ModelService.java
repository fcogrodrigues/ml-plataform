package com.ifood.mlplatform.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import smile.data.type.StructType;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifood.mlplatform.model.Predictable;
import com.ifood.mlplatform.model.dto.SmileAdapter;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@Slf4j
public class ModelService {

    private Predictable model;
    private StructType schema;

    @PostConstruct
    public void init() {
        Path modelPath = Path.of("model/model.bin");
        Path schemaPath = Path.of("model/schema.bin");

        if (!Files.exists(modelPath)) {
            throw new IllegalStateException("❌ model/model.bin not found. Run TrainModel before starting the API.");
        }
        if (!Files.exists(schemaPath)) {
            throw new IllegalStateException("❌ model/schema.json not found. Run TrainModel before starting the API.");
        }

        try {
            // Carregar o modelo serializado
            Serializable loadedModel;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelPath.toFile()))) {
                Object obj = ois.readObject();
                if (!(obj instanceof Serializable)) {
                    throw new IllegalStateException("The loaded object is not Serializable.");
                }
                loadedModel = (Serializable) obj;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(schemaPath.toFile()))) {
                Object obj = ois.readObject();
                if (!(obj instanceof StructType)) {
                    throw new IllegalStateException("The loaded object is not a StructType schema.");
                }
                schema = (StructType) obj;
            }

            // Inicializar o SmileAdapter com modelo e schema
            this.model = new SmileAdapter(loadedModel, schema);

            log.info("✅ Model loaded successfully from {}", modelPath.toAbsolutePath());
            log.info("✅ Schema loaded successfully from {}", schemaPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("❌ Error loading model or schema: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load the model and schema.", e);
        }
    }

    public Object predict(List<Double> features) {
        double[] featureArray = features.stream().mapToDouble(Double::doubleValue).toArray();
        Object prediction = model.predict(featureArray);
        log.info("✅ Prediction completed for features {} -> {}", features, prediction);
        return prediction;
    }
}
