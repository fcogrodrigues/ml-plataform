package com.ifood.mlplatform.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import smile.data.Tuple;
import smile.data.type.DataType;
import smile.data.type.StructType;

import org.springframework.stereotype.Service;

import com.ifood.mlplatform.model.Predictable;
import com.ifood.mlplatform.model.dto.SmileAdapter;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

@Service
@Slf4j
public class ModelService {

    private Predictable model;
    private StructType schema;

    // Assuming S3StorageService is a service to interact with S3 storage
    private final StorageService storageService;
    
    public ModelService(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostConstruct
    public void init() {
        log.info("🔧 Loading from model and schema...");
        try (
            InputStream modelStream = storageService.download("model.bin");
            ObjectInputStream modelOis = new ObjectInputStream(modelStream);
            
            InputStream schemaStream = storageService.download("schema.bin");
            ObjectInputStream schemaOis = new ObjectInputStream(schemaStream)
        ) {
            Object modelObj = modelOis.readObject();
            if (!(modelObj instanceof Serializable)) {
                throw new IllegalStateException("The loaded model is not Serializable.");
            }

            StructType schemaObj = (StructType) schemaOis.readObject();
            this.schema = schemaObj;

            this.model = new SmileAdapter((Serializable) modelObj, schemaObj);

            log.info("✅ Model and Schema loaded successfully from Storage");
        } catch (Exception e) {
                log.error("❌ Error loading schema from Storage: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to load schema from Storage.", e);
        }
    }

    public Object predict(List<Double> features) {
        double[] featureArray = features.stream().mapToDouble(Double::doubleValue).toArray();
        Object prediction = model.predict(featureArray);
        log.info("✅ Prediction completed for features {} -> {}", features, prediction);
        return prediction;
    }
}
