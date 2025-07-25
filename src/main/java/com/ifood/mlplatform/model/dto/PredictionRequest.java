package com.ifood.mlplatform.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class PredictionRequest {
    private List<Double> features;
}