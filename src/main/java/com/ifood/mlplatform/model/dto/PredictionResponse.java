package com.ifood.mlplatform.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor    
@AllArgsConstructor   
public class PredictionResponse {

    /**
     * The prediction result (e.g. a class label or numeric value).
     */
    @JsonProperty("prediction")
    private Object prediction;
}
