package com.ifood.mlplatform.controller;

import com.ifood.mlplatform.model.dto.PredictionRequest;
import com.ifood.mlplatform.model.dto.PredictionResponse;
import com.ifood.mlplatform.service.ModelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/predict")
@RequiredArgsConstructor
@Tag(name = "Model Prediction API", description = "Serve predictions for ML models")
public class PredictionController {

    private final ModelService modelService;

    @PostMapping("/{modelId}")
    @Operation(summary = "Make prediction using modelId")
    public ResponseEntity<PredictionResponse> predict(
                @PathVariable("modelId") String modelId,
                @RequestBody PredictionRequest request) {
        Object prediction = modelService.predict(modelId, request.getFeatures());
        return ResponseEntity.ok(new PredictionResponse(prediction));
    }

}
