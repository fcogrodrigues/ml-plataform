package com.ifood.mlplatform.controller;

import com.ifood.mlplatform.model.dto.PredictionRequest;
import com.ifood.mlplatform.model.dto.PredictionResponse;
import com.ifood.mlplatform.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/predict", 
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Model Prediction API", description = "Serve predictions for ML models")
public class PredictionController {

    private final ModelService modelService;

    @PostMapping("/{modelId}")
    @Operation(
      summary = "Make a prediction with the given model",
      parameters = {
        @Parameter(name = "modelId", description = "Identifier of the model to use", required = true)
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Feature values keyed by name",
        required = true,
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = PredictionRequest.class),
          examples = @ExampleObject(
            name = "Iris sample",
            value = """
              {
                "features": {
                  "sepal_length": 5.1,
                  "sepal_width":  3.5,
                  "petal_length": 1.4,
                  "petal_width":  0.2
                }
              }
              """
          )
        )
      ),
      responses = {
        @ApiResponse(responseCode = "200", description = "Prediction successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing feature"),
        @ApiResponse(responseCode = "404", description = "Model not found")
      }
    )
    public ResponseEntity<PredictionResponse> predict(
            @PathVariable String modelId,
            @Valid @RequestBody PredictionRequest request) {

        Object prediction = modelService.predict(modelId, request.getFeatures());
        return ResponseEntity.ok(new PredictionResponse(prediction));
    }
}
