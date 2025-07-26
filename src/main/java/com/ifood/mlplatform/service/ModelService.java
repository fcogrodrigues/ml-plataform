package com.ifood.mlplatform.service;

import com.ifood.mlplatform.util.MetadataConverter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import smile.data.type.StructType;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifood.mlplatform.model.Predictable;
import com.ifood.mlplatform.model.dto.SmileAdapter;
import com.ifood.mlplatform.model.metadata.ModelMetadata;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelService {

    private final StorageService storage;

    private final Map<String, LoadedModel> modelCache = new ConcurrentHashMap<>();

    private record LoadedModel(Predictable model, StructType schema, ModelMetadata metadata) {}

    private Predictable model;
    private StructType schema;
    private ModelMetadata modelMetadata;
    
    private LoadedModel loadModel(String modelId) {
        log.info("📦 Loading model with ID: {}", modelId);

        try (
            InputStream modelStream = storage.download(modelId + "/model.bin");
            InputStream schemaStream = storage.download(modelId + "/schema.json");
        ) {
            ObjectInputStream modelOis = new ObjectInputStream(modelStream);
            Object modelObj = modelOis.readObject();
            if (!(modelObj instanceof Serializable)) {
                throw new IllegalStateException("The loaded model is not Serializable.");
            }

            ObjectMapper mapper = new ObjectMapper();
            ModelMetadata metadata = mapper.readValue(schemaStream, ModelMetadata.class);
            StructType schema = MetadataConverter.toStructType(metadata);
            Predictable model = new SmileAdapter((Serializable) modelObj, schema);

            log.info("✅ Loaded model: {}, features: {}", modelId, metadata.features.size());

            return new LoadedModel(model, schema, metadata);        
        } catch (Exception e) {
            log.error("❌ Failed to load model {}: {}", modelId, e.getMessage(), e);
            throw new RuntimeException("Could not load model: " + modelId, e);
        }
    }

    public Object predict(String modelId, Map<String, Object> features) {
      
        LoadedModel loadedModel = modelCache.computeIfAbsent(modelId, this::loadModel);

        ModelMetadata metadata = loadedModel.metadata();
        
        List<ModelMetadata.Feature> featureDefs = metadata.features;
        double[] featureArray = new double[featureDefs.size()];

        for (int i = 0; i < featureDefs.size(); i++) {
            String featureName = featureDefs.get(i).name;
            Object value = features.get(featureName);

            if (value == null) {
                throw new IllegalArgumentException("Missing feature: " + featureName);
            }

            try {
                featureArray[i] = (value instanceof Number)
                        ? ((Number) value).doubleValue()
                        : Double.parseDouble(value.toString());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid value for feature " + featureName + ": " + value, e);
            }
        }

        Object rawPrediction = loadedModel.model.predict(features);

        // Traduz o índice para classe nominal
        if (rawPrediction instanceof Integer && metadata.label != null && metadata.label.classes != null) {
            int index = (Integer) rawPrediction;
            List<String> classes = metadata.label.classes;
            if (index >= 0 && index < classes.size()) {
                return classes.get(index);
            }
        }

        return rawPrediction;
    }
}
