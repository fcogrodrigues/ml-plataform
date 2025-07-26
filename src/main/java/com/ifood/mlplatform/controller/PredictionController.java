package com.ifood.mlplatform.controller;

import com.ifood.mlplatform.model.dto.PredictionRequest;
import com.ifood.mlplatform.model.dto.PredictionResponse;
import com.ifood.mlplatform.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/predict")
@RequiredArgsConstructor
public class PredictionController {

    private final ModelService modelService;

    @PostMapping
    public ResponseEntity<PredictionResponse> predict(@RequestBody PredictionRequest request) {
        Object prediction = modelService.predict(request.getModelId(), request.getFeatures());
        return ResponseEntity.ok(new PredictionResponse(prediction));
    }

}
