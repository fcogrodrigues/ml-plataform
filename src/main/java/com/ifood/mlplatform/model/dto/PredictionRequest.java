package com.ifood.mlplatform.model.dto;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(
  name = "PredictionRequest",
  description = "Request payload containing a map of feature names to their values"
)
public class PredictionRequest {

    @NotNull(message = "Features map must be provided")
    @NotEmpty(message = "Features map cannot be empty")
    @Schema(
      description = "Key-value map of feature names to numeric values",
      example = "{\"sepal_length\":5.1, \"sepal_width\":3.5, \"petal_length\":1.4, \"petal_width\":0.2}"
    )
    private Map<String, Object> features;
}
