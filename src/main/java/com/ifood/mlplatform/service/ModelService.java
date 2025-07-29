package com.ifood.mlplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifood.mlplatform.exception.ModelNotFoundException;
import com.ifood.mlplatform.model.ModelAdapter;
import com.ifood.mlplatform.model.Predictable;
import com.ifood.mlplatform.model.metadata.ModelMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelService {

    private final StorageService storage;
    private final AdapterFactory adapterFactory;

    private final ConcurrentHashMap<String, LoadedModel> modelCache = new ConcurrentHashMap<>();

    /**
     * Load, cache and return a Predictable adapter for this modelId.
     * Wrap any failure (I/O / JSON / adapter‐lookup) as a ModelNotFoundException.
     */
    private LoadedModel loadModel(String modelId) {
        log.info("📦 Loading model `{}`", modelId);

        try (
            InputStream modelStream    = storage.download(modelId + "/model.bin");
            InputStream metadataStream = storage.download(modelId + "/schema.json")
        ) {
            Serializable rawModel = deserializeModel(modelStream);

            // readValue will close metadataStream when done
            ModelMetadata metadata = new ObjectMapper()
                .readValue(metadataStream, ModelMetadata.class);

            ModelAdapter adapter = adapterFactory.getAdapter(metadata);
            Predictable predictor = adapter.load(rawModel, metadata);

            log.info("✅ Loaded `{}` (framework={}, {} features)",
                     modelId, metadata.framework, metadata.features.size());

            return new LoadedModel(predictor, metadata);

        } catch (Exception e) {
            log.error("❌ Error loading model `{}`: {}", modelId, e.getMessage());
            throw new ModelNotFoundException(modelId);
        }
    }

    /**
     * Public entrypoint: score a feature‐map against the named model.
     * Any missing‐feature or parse errors bubble as IllegalArgumentException (→ 400),
     * ModelNotFoundException bubbles as 404.
     */
    public Object predict(String modelId, Map<String, Object> features) {
        LoadedModel lm = modelCache.computeIfAbsent(modelId, this::loadModel);
        return lm.predict(features);
    }

    /**
     * Deserialize a Java‐serialized model from an InputStream.
     */
    private Serializable deserializeModel(InputStream in) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(in)) {
            Object obj = ois.readObject();
            if (!(obj instanceof Serializable)) {
                throw new IllegalStateException("Model is not Serializable");
            }
            return (Serializable) obj;
        }
    }

    /**
     * Simple holder for a loaded model + its metadata.
     */
    private static record LoadedModel(Predictable predictor,
                                      ModelMetadata metadata) {

        /**  
         * Validate & invoke the underlying adapter  
         * (any Invalid format → IllegalArgumentException)  
         * (any numeric → class mapping is done in the adapter itself)  
         */
        Object predict(Map<String, Object> features) {
            return predictor.predict(features);
        }
    }
}
