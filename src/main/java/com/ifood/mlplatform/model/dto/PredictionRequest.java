package com.ifood.mlplatform.model.dto;

import java.util.Map;

import lombok.Getter;

@Getter
public class PredictionRequest {
    private String modelId;
    private Map<String, Object> features;
}