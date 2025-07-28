package com.ifood.mlplatform.service;

import com.ifood.mlplatform.exception.ModelNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifood.mlplatform.model.ModelAdapter;
import com.ifood.mlplatform.model.Predictable;
import com.ifood.mlplatform.model.metadata.ModelMetadata;

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

    private final Map<String, LoadedModel> modelCache = new ConcurrentHashMap<>();

    private record LoadedModel(Predictable adapter, ModelMetadata metadata) {}

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

            ModelMetadata metadata = new ObjectMapper().readValue(schemaStream, ModelMetadata.class);
            ModelAdapter adapterDefined = adapterFactory.getAdapter(metadata);
            Predictable adapter  = adapterDefined.load((Serializable) modelObj, metadata);

            log.info("✅ Model `{}` (framework={}) loaded with {} features",
                     modelId, metadata.framework, metadata.features.size());


            return new LoadedModel(adapter, metadata);
        } catch (ModelNotFoundException e) {
            // already throwed by factory or storage
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to load model {}: {}", modelId, e.getMessage());
            throw new ModelNotFoundException(modelId);
        }
    }

    public Object predict(String modelId, Map<String, Object> features) {
     
        LoadedModel lm = modelCache.computeIfAbsent(modelId, this::loadModel);
        return lm.adapter().predict(features);

    }
}
