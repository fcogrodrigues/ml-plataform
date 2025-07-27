package com.ifood.mlplatform.model.dto;

import java.util.Map;

import lombok.Getter;

@Getter
public class PredictionRequest {
    private Map<String, Object> features;
}