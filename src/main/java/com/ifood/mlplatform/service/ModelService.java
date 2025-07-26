package com.ifood.mlplatform.service;

import com.ifood.mlplatform.util.MetadataConverter;

import jakarta.annotation.PostConstruct;
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

@Service
@Slf4j
public class ModelService {

    private final StorageService storageService;

    private Predictable model;
    private StructType schema;
    private ModelMetadata modelMetadata;
    
    public ModelService(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostConstruct
    public void init() {
        log.info("🔧 Loading model and schema from storage...");
        try (
            InputStream modelStream = storageService.download("model.bin");
            InputStream schemaStream = storageService.download("schema.json");
        ) {
            ObjectInputStream modelOis = new ObjectInputStream(modelStream);
            Object modelObj = modelOis.readObject();
            if (!(modelObj instanceof Serializable)) {
                throw new IllegalStateException("The loaded model is not Serializable.");
            }

            ObjectMapper mapper = new ObjectMapper();

            this.modelMetadata = mapper.readValue(schemaStream, ModelMetadata.class);
            log.info("✅ schema.json loaded with {} features", modelMetadata.features.size());

            this.schema = MetadataConverter.toStructType(modelMetadata);
            this.model = new SmileAdapter((Serializable) modelObj, schema);

            log.info("✅ Model and Schema loaded successfully from Storage");
        } catch (Exception e) {
                log.error("❌ Error loading schema from Storage: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to load schema from Storage.", e);
        }
    }

    public Object predict(Map<String, Object> featureMap) {
        List<ModelMetadata.Feature> featureDefs = modelMetadata.features;
        double[] featureArray = new double[featureDefs.size()];

        for (int i = 0; i < featureDefs.size(); i++) {
            String featureName = featureDefs.get(i).name;
            Object value = featureMap.get(featureName);

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

        Object rawPrediction = model.predict(featureMap);

        // Traduz o índice para classe nominal
        if (rawPrediction instanceof Integer && modelMetadata.label != null && modelMetadata.label.classes != null) {
            int index = (Integer) rawPrediction;
            List<String> classes = modelMetadata.label.classes;
            if (index >= 0 && index < classes.size()) {
                return classes.get(index);
            }
        }

        return rawPrediction;
    }

}
