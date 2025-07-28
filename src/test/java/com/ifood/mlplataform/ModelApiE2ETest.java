package com.ifood.mlplataform;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class ModelApiE2ETest {

    private static String MODEL_ID;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = System.getenv().getOrDefault("RESTASSURED_BASE_URI", "http://localhost");
        RestAssured.port    = Integer.parseInt(System.getenv().getOrDefault("RESTASSURED_PORT", "8080"));    
        MODEL_ID = System.getenv().getOrDefault("MODEL_ID", "iris");
    }

    @Test
    void testSuccessfulPrediction() {
        given()
            .contentType("application/json")
            .body("""
                {
                  "features": {
                    "sepal_length": 5.1,
                    "sepal_width": 3.5,
                    "petal_length": 1.4,
                    "petal_width": 0.2
                  }
                }
                """)
        .when()
            .post("/predict/{modelId}", MODEL_ID)
        .then()
            .statusCode(200)
            .body("prediction", equalTo("setosa"));
    }

    @Test
    void testModelNotFound() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "features": {
                        "sepal_length": 5.1,
                        "sepal_width": 3.5,
                        "petal_length": 1.4,
                        "petal_width": 0.2
                    }
                }
                """)
        .when()
            .post("/predict/{modelId}", "nonexistent-model")
        .then()
            .statusCode(404)
            .body("message", equalTo("Model not found: nonexistent-model"));
    }

    @Test
    void testMissingFeatureInSchema() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "features": {
                        "sepal_length": 5.1,
                        "sepal_width": 3.5,
                        "petal_width": 0.2
                    }
                }
                """) // petal_length está faltando
        .when()
            .post("/predict/{modelId}", MODEL_ID)
        .then()
            .statusCode(400)
            .body("message", containsString("Missing feature: petal_length"));
    }

    @Test
    void testInvalidFeatureValueFormat() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "features": {
                        "sepal_length": "abc",
                        "sepal_width": 3.5,
                        "petal_length": 1.4,
                        "petal_width": 0.2
                    }
                }
                """)
        .when()
            .post("/predict/{modelId}", MODEL_ID)
        .then()
            .statusCode(400)
            .body("message", containsString("Invalid value for feature"));
    }


}
