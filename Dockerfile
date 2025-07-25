# ======================================
# Build Stage
# ======================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY . .

# Build usando o perfil `api`
RUN mvn clean package -Papi -DskipTests

# ======================================
# Runtime Stage
# ======================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia o jar gerado pelo perfil `api`
COPY --from=builder /app/target/mini-ml-platform-1.0.0.jar app.jar
COPY model/model.bin model/model.bin

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]