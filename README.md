# Mini ML Plataform

Automated pipeline for training and serving machine learning models via REST API

## 🚀 Features

- ✅ Train models on arbitrary CSV data (example: Iris dataset)

- ✅ Upload trained model and schema metadata to S3-compatible storage (MinIO)

- ✅ Dynamically load and serve multiple models by `modelId`

- ✅ Generic API `POST (/predict/{modelId})` for scoring new observations

- ✅ Automatic schema validation and feature parsing

- ✅ End-to-end integration tests (JUnit + RestAssured)

- ✅ Swagger/OpenAPI UI for interactive API exploration

---

## ⚙️ Tech Stack

- **Java 21**, Spring Boot 3.x

- **Smile** machine learning framework (for trainer and predictions)

- **MinIO** (local S3-compatible storage)

- **JUnit 5**, **RestAssured** for E2E tests

- **Maven** + **Docker** + **Docker Compose**

---

## 📂 Requirements

To build and run locally, you need:

- ✅ **Java 21** [Download](https://www.oracle.com/java/technologies/downloads/#java21)

- ✅ **Maven 3.9+** [Download](https://maven.apache.org/download.cgi)

- ✅ **Docker** [Download](https://www.docker.com/products/docker-desktop/)

---

## 🛠️ Quick Start

The provided `docker-compose.yml` will orchestrate:

1. **MinIO** (Storage)

2. **ML API** service

3. **Model Trainer**

4. **E2E test runner**

### 1️⃣ Clone & enter project

```bash
git clone https://github.com/fcogrodrigues/ml-platform.git
cd ml-platform
```

### 2️⃣ Build & Run Everything

```bash
docker-compose up --build
```

API `http://localhost:8080`

Swagger UI `http://localhost:8080/swagger-ui/index.html`

MinIO console `http://localhost:9001`

On startup, the iris model is automatically trained, uploaded under key `iris/` and ready to query.

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

### 🔄 Rebuild & Ru trainer Only

To train a new model with custom parameters:

```bash
docker-compose run --rm \
  -e CSV_PATH=/data/other.csv \
  -e SCHEMA_PATH=/data/other_schema.json \
  -e MODEL_ID=newmodelv1 \
  ml-trainer
```

This saves to Storage:

```plaintext
model/
├── newmodelv1/
│   ├── model.bin
│   ├── schema.json
```

---

## 📝 API Endpoints

### POST /predict/{modelId}

Score a set of features against the named model.

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
{ 
  "message": "Missing feature: petal_width" 
}
```

- **400 Bad Request** - Malformed JSON

```bash
{ 
  "message": "Malformed JSON request"
}
```

- **404 Not Found** – unknown modelId

```bash
{ 
  "message": "Model not found: unknown-model" 
}
```

- **415 Unsupported Content-Type** – Unsupported Content-Type (e.g. not JSON)

```bash
{ 
  "message": "message": "Content type 'text/plain' not supported"
}
```

- **503 Service Unavaiable** – Service Unavaiable

```bash
{ 
  "message": "message": "Failed to load key: iris/model.bin"
}
```

- **500 Unexpected server error** – Unexpected server error

```bash
{ 
  "message": "Unexpected error occurred"
}
```

*Additional error types can be added via the global exception handler.*

## 🎨 Architecture & Design Decisions

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

- Support for arbitrary feature sets (numerical, categorical [not in this MVP], text etc),

- Decoupling the prediction logic from the controller/service layer,

- Flexibility in integrating different model types and frameworks without changing the API surface,

- Simplified input handling via generic key-value maps.

⚠️ Trade off: While the current implementation returns a single prediction (scalar or class), this method could be extended to return richer payloads—such as multiple outputs or probabilities—in alignment with a more generic `metadata` schema as described in section 1️⃣.

### 4️⃣ In‑Memory Model Cache

Once a model is loaded (from bucket), it’s held in a cache (`ConcurrentHashMap`) keyed by modelId, so subsequent calls reuse the same object.

Minimizes I/O and deserialization overhead under load, improving throughput and reducing latency.

### 6️⃣ Bucket for Artifact Storage

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
- ✅ Keeps controller code clean and free of boilerplate

⚠️ Trade-off: Due to time constraints, not all possible exception types were implemented. However, the error-handling structure is extensible and ready to support additional scenarios with minimal effort.

### Generals Trade-offs & Future Improvements

- 🚫 **Authentication/Authorization** was not implemented for this MVP.

- 🚫 **Logs, Monitoring and Metrics** not included in this MVP; these can be added later for **operational observability**.

- 🚫 **Distributed Cache** not included in this MVP; these can be added later for Redis use

- 🚫 **Unit Tests or coverage tests** not included fully in this MVP; Anyway, the main E2E tests are avaiable.

---

## 📦 AWS Architecture proposal

### API Service Pipeline

  1. Developer pushes code (GitHub) → **AWS CodePipeline**

  2. **CodePipeline**

      - Invokes **CodeBuild** to build & test the ML API container image

      - On success, **CodeDeploy** pushes the image to **ECR** and updates the running service on **ECS  (Fargate)/EKS** and ensures the Redis cluster is in place.

  4. **API Gateway**

      - Fronts the ML API service for routing, authentication, and rate‑limiting

  5. **ML API Service** (ECS/EKS)

      - Serves `/predict/{modelId}`

      - **At request time**, first checks **Redis cache (ElastiCache)** for:

        - Serialized schema & model metadata

        - Recent predictions (optional)

      - Falls back to **S3** if cache miss, and populates **Redis** for subsequent calls.

  6. **CloudWatch**

      - Collects logs and metrics from the API containers and Redis cluster.

      - Alarms on high latency, error rates or cache miss spikes.

### Training Service Pipeline

  1. EventBridge (cron scheduled or manual trigger)

      - fires an event when (re)training is desired.

  2. **Lambda**

      - Receives the **EventBridge** event

      - Calls **ECS (Fargate)** to launch a Trainer task

  3. **Fargate Trainer Task**

      - Runs the trainer job inside a container (docker)

      - Reads raw (dataset) data (e.g. from S3, RDS, databricks, etc.) and the corresponding `schema.json`

      - Trains a model, serializes the model + updated `schema.json`

      - Uploads artifacts to **S3** under `<modelId>/model.bin` (can also .pmml ou .onnx in na future version) and `<modelId>/schema.json`. Invalidate cache on Redis.

  4. **ModelService**

      - On the next prediction request, sees the cache miss (invalidated), reloads the fresh model + schema from S3, and repopulates Redis.

  5. **CloudWatch**
      - Collects logs, metrics, and alarms from the trainer jobs

### Why Each Component?

  - **API Gateway**: uniform ingress, auth, throttling, easy domain management.

  - **ECS/EKS (Fargate)**: auto‑scaling, no server management, pay‑per‑use.

  - **ElastiCache Redis**:

    - **Low‑latency** access to model metadata & hot predictions.

    - **Cache invalidation** on retraining keeps API lightweight.

  - **EventBridge & Lambda**:

    - **Decoupled**, event‑driven triggers for on‑demand (cost-eficient) or scheduled training.

  - **CodePipeline / CodeBuild / ECR / CodeDeploy**:

    - **Automated CI/CD** for both API and Trainer, with testing & version control. This ensure 

  - **CloudWatch**: unified logging, metrics, alarms for both API & cache

---

## 📂 Project Structure

```plaintext
mini-ml-platform/
├── src/
│   ├── main/java/com/ifood/mlplatform/
│   │   ├── controller/       # REST endpoints
|   |   ├── exception         # Clean error handling
│   │   ├── model/            # Metadata & adapter interfaces
│   │   ├── service/          # StorageService, ModelService
│   │   ├── training/         # TrainModel CLI
│   │   ├── util/             # Metadata Converter
│   └── resources/
│       └── data/             # iris.csv, schema.json
├── docker-compose.yml
├── Dockerfile.api
├── Dockerfile.trainer
├── Dockerfile.test-e2e
├── pom.xml
└── README.md
```

### 📂 Explanation of Structure

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

## BONUS!!!

### level01

```bash
passwd: "1F00d{P1c3_0f_C4k3}"
```

### level02

```bash
passwd: "not the passwordaacrB"
```

### level03

The challenger was not done, but is a technique to use a buffer overflow to exploit a vulnerability to call a hidden (crazy) function.
