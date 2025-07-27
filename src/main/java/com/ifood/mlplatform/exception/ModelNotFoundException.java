package com.ifood.mlplatform.exception;

public class ModelNotFoundException extends RuntimeException {
    public ModelNotFoundException(String modelId) {
        super("Model not found: " + modelId);
    }
}
