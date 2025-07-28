package com.ifood.mlplataform;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class ModelApiE2ETest {

    private final String MODEL_ID = "daise4";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
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
            .post("/predict/" + MODEL_ID)
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
            .post("/predict/nonexistent-model")
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
            .post("/predict/" + MODEL_ID)
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
            .post("/predict/" + MODEL_ID)
        .then()
            .statusCode(400)
            .body("message", containsString("Invalid value for feature"));
    }


}
