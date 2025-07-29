# 🪙 Mini ML Plataform

Automated pipeline for training and serving machine learning models via REST API

## 🚀 Features

✅ Train models on arbitrary CSV data (example: Iris dataset)

✅ Upload trained model and schema metadata to S3-compatible storage (MinIO)

✅ Dynamically load and serve multiple models by modelId

✅ Generic API (/predict/{modelId}) for scoring new observations

✅ Automatic schema validation and feature parsing

✅ End-to-end integration tests (JUnit + RestAssured)

✅ Swagger/OpenAPI UI for interactive API exploration

---

## ⚙️ Tech Stack

- Java 21, Spring Boot 3.x

- Smile machine learning framework

- MinIO (S3-compatible storage) via io.minio SDK

- JUnit 5, RestAssured for E2E tests

- Maven + Docker + Docker Compose

---

## 📂 Requirements

To build and run this Wallet Service locally, you need:

✅ **Java 21** [Download](https://www.oracle.com/java/technologies/downloads/#java21)

✅ **Maven 3.8+** [Download](https://maven.apache.org/download.cgi)

✅ **Docker** [Download](https://www.docker.com/products/docker-desktop/)

---

## 🛠️ Quick Start

The `docker-compose.yml` orchestrates:

1. MinIO (Storage)

2. API service (ML Platform)

3. Model Trainer

4. E2E test runner

1️⃣ Clone and enter project

```bash
git clone https://github.com/fcogrodrigues/ml-platform.git
cd ml-platform
```

2️⃣ Bring up Storage + API + Trainer + E2E Tests

```bash
docker-compose up --build
```

MinIO (S3‑compatible storage) will be available at `http://localhost:9001` (console).

API available at `http://localhost:8080`

Swagger UI at `http://localhost:8080/swagger-ui/index.html`

After startup, the iris model is automatically trained (via ml-trainer) and uploaded under `iris/` key in bucket; you can then call the iris endpoint.

```bash
curl -X POST http://localhost:8080/predict/iris \
     -H 'Content-Type: application/json' \
     -d '{
           "features": {
             "sepal_length": 5.1,
             "sepal_width": 3.5,
             "petal_length": 1.4,
             "petal_width": 0.2
           }
         }'
```

---

🔄 Rebuild & run trainer only

This executes `ml-trainer` with news parameters for training new models if desired. Publish the new model in bucket `/model/new_model/...`.

```bash
docker-compose run --rm \
  -e CSV_PATH=/data/other.csv \
  -e SCHEMA_PATH=/data/other_schema.json \
  -e MODEL_ID=newmodelv1 \
  ml-trainer
```

The Storage will be:

```plaintext
model/
├── newmodelv1/
│   ├── model.bin
│   ├── schema.json
```

## 📝 API Endpoints

### POST /predict/{modelId}

Score a batch of features against the named model ID

```bash
curl -X POST http://localhost:8080/predict/iris \
     -H 'Content-Type: application/json' \
     -d '{
           "features": {
             "sepal_length": 5.1,
             "sepal_width": 3.5,
             "petal_length": 1.4,
             "petal_width": 0.2
           }
         }'
```

Responses

- **200 OK** - successful prediction

```bash
{
 "prediction": "setosa" 
}
```

- **400 Bad Request** - missing feature or invalid value

```bash
{ "message": "Missing feature: petal_width" }
```

- **404 Not Found** – unknown modelId

```bash
{ "message": "Model not found: unknown-model" }
```

**Important**: Other errors were not implemented due to time constraints, but any additional error can be easily handled using the error handler.

## 🎨 Architecture & Design Decisions

### 🚀 Summary

Generic Predictable interface for pluggable adapters

ModelMetadata JSON defines features, types and class labels

AdapterFactory selects correct adapter by framework field

MetadataConverter builds Smile StructType schema from metadata ()

Concurrent cache for loaded models, avoids reload overhead

Docker Compose used for CI/CD-like local pipeline orchestration

### 1️⃣ Generic, JSON‑based Model Metadata

Each model is shipped with a corresponding `schema.json` file that, in this implementation, describes the following:

- `framework`: the ML library used (e.g., Smile, TensorFlow, scikit-learn),

- `model_type`: the type of model (e.g., RandomForest, LogisticRegression),

- `features`: an array of objects specifying each input's name and type,

- `label`: an object defining the prediction target with name, type, and a mapping of numeric outputs to human-readable class names.

This label mapping ensures that consumers receive clear, interpretable predictions instead of raw numeric values.

⚠️ Note: A more generic metadata format could support many additional components—such as hyperparameters, multi-output predictions, and categorical or binary targets (e.g., "M" vs. "F", "Yes" vs. "No"). However, due to time constraints, these features were not included in the current implementation.

By explicitly defining the model interface, this approach decouples the API from any specific ML library (Smile, TensorFlow, scikit-learn, etc.), and makes the integration contract fully transparent. Clients know exactly which fields are required, in what format, and how to interpret the resulting predictions.

### 2️⃣ Adapter Pattern (ModelAdapter + AdapterFactory)

The ModelAdapter defines a simple contract for framework‐specific operations—namely, `load()`—and is backed by an AdapterFactory that chooses the right implementation based on the `metadata.framework` field.

Each adapter encapsulates all ML‐runtime details, including:

- Model deserialization

- Schema binding

- Invocation of the prediction call

⚠️ Trade off: In our MVP we only look at the framework property to pick an adapter. In a future version you could also consider other metadata (e.g. model_type or algorithm parameters) to perform even finer‐grained selection.

Because the core service only depends on the `ModelAdapter` abstraction, you can add support for new runtimes (Spark MLlib, TensorFlow, etc.) without touching the service logic. This follows the **Open/Closed Principle**: the system is open for extension (add new adapters) but closed for modification (no changes to existing code).

⚠️ Trade off: Right now the MVP serializes models as `.bin` files to satisfy Smile’s API, but the same adapter pattern would let us easily switch to a more interoperable format—PMML or ONNX—so that models become portable across languages and frameworks.

### 3️⃣ Uniform `Predictable` Interface (`Map<String, Object> → Object`)

All adapters implement a common interface:

```java
Object predict(Map<String, Object> features);
```

This uniform contract allows:

- Support for arbitrary feature sets (numerical, categorical, text),

- Decoupling the prediction logic from the controller/service layer,

- Flexibility in integrating different model types and frameworks without changing the API surface,

- Simplified input handling via generic key-value maps.

⚠️ Trade off: While the current implementation returns a single prediction (scalar or class), this method could be extended to return richer payloads—such as multiple outputs or probabilities—in alignment with a more generic `metadata` schema as described in section 1️⃣.

### 4️⃣ In‑Memory Model Cache

Once a model is loaded (from bucket), it’s held in a cache (`ConcurrentHashMap`) keyed by modelId, so subsequent calls reuse the same object.

Minimizes I/O and deserialization overhead under load, improving throughput and reducing latency.

### 6️⃣ MinIO for Artifact Storage

Models (`model.bin`) and metadata (`schema.json`) are stored in an S3-compatible bucket served by MinIO (running locally via Docker Compose), simulating AWS S3 behavior.

Why this approach?

- ✅ Lightweight and local development dependency

- ✅ Parity with AWS S3 used in production

- ✅ Clean separation of storage concerns from the API runtime

- ✅ Enables secure, versioned artifact storage and retrieval

This setup allows seamless switching between local and cloud environments without changing application logic.

### 7️⃣ Centralized Exception Handling & Validation

A global exception handler is implemented to catch and format errors consistently, along with input validation to detect missing or malformed features.

Why this matters:

- ✅ Provides uniform and descriptive error responses
  – `400 Bad Request` for invalid client input
  – `404 Not Found` when a given modelId does not exist
- ✅ Keeps controller code clean and free of boilerplate

⚠️ Trade-off: Due to time constraints, not all possible exception types were implemented. However, the error-handling structure is extensible and ready to support additional scenarios with minimal effort.

### Generals Trade-offs & Future Improvements

- 🚫 **Authentication/Authorization** was not implemented for this MVP.

- 🚫 **Logs, Monitoring and Metrics** not included in this MVP; these can be added later for **operational observability**.

---

## 📂 Project Structure

```plaintext
mini-ml-platform/
├── src/
│   ├── main/java/com/ifood/mlplatform/
│   │   ├── controller/       # REST endpoints
│   │   ├── model/            # Metadata & adapter interfaces & API request/response models
|   |   ├── exception         # Clean error handling
│   │   ├── service/          # Storage, ModelService
│   │   ├── training/         # TrainModel CLI
│   │   ├── util/             # Utilities (metadata converter)
│   │   └── MiniMlPlatformApplication.java
│   └── resources/
│       ├── data/iris.csv
│       └── data/schema.json
├── docker-compose.yml
├── Dockerfile.api
├── Dockerfile.test-e2e
├── Dockerfile.trainer
├── pom.xml
└── README.md
```

### 🧩 Explanation of Structure

- **controller/** – Defines REST endpoints, delegating business logic to services.
- **model/** – Domain entities encapsulating business rules and request/response contracts.
- **exception/** – Clean error handling.
- **service/** – Core business logic, unit-tested and reusable.
- **training/** - TrainModel Client
- **util/** – Utilities classes.

---

## 👤 Author

### **Francisco Rodrigues**

Feel free to contact for clarifications or contributions.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-blue?logo=linkedin)](https://www.linkedin.com/in/fcorodrigues/)
