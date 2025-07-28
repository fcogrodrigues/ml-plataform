package com.ifood.mlplatform.model;

import java.io.Serializable;

import com.ifood.mlplatform.model.metadata.ModelMetadata;

public interface ModelAdapter {
    /** 
     * Returns true if this adapter knows to load and to set
     * models from metadata.framework
     */
    boolean supports(ModelMetadata metadata);
    Predictable load(Serializable rawModel, ModelMetadata metadata);
}
