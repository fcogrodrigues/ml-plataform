package com.ifood.mlplatform.training;

import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.type.StructType;
import smile.io.CSV;
import smile.data.vector.IntVector;

import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TrainModel {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java -jar mini-ml-platform-1.0.0-jar-with-dependencies.jar <path-to-csv>");
            System.exit(1);
        }

        String csvPath = args[0];
        Path path = Path.of(csvPath);
        if (!Files.exists(path)) {
            throw new IllegalStateException("❌ File data/iris.csv not found.");
        }
        System.out.println("Starting training job...");
        System.out.println("Reading data from: " +  csvPath);

        DataFrame data = new CSV().read(csvPath);

        System.out.println("Columns found: ");
        for (String name : data.names()) {
            System.out.println(" - " + name);
        }

        String labelColumn = "V5";
        String[] species = data.stringVector(labelColumn).distinct().toArray(new String[0]);
        Map<String, Integer> speciesMap = new HashMap<>();
        for (int i = 0; i < species.length; i++) {
            speciesMap.put(species[i], i);
        }

        String[] labelValues = data.column(labelColumn).toStringArray();
        int[] labels = new int[labelValues.length];
        for (int i = 0; i < labelValues.length; i++) {
            labels[i] = speciesMap.get(labelValues[i]);
        }

        data = data.drop(labelColumn).merge(IntVector.of(labelColumn, labels));

        System.out.println("Label mapping applied: " + speciesMap);

        Formula formula = Formula.lhs(labelColumn);

        System.out.println("Starting model training with label column: " + labelColumn);

        RandomForest model = RandomForest.fit(formula, data);

        System.out.println("Model training completed. Saving model...");

        Path modelDir = Path.of("model");
        if (!Files.exists(modelDir)) {
            Files.createDirectories(modelDir);
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(modelDir.resolve("model.bin")))) {
            oos.writeObject(model);
        }
        
        System.out.println("✅ Model saved to " + modelDir.resolve("model.bin").toAbsolutePath());
        StructType schema = data.schema();
        Path schemaPath = Path.of("model/schema.bin");
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(schemaPath))) {
            oos.writeObject(schema);
        }

        System.out.println("✅ Model saved to " + modelDir.resolve("model.bin").toAbsolutePath());
        System.out.println("✅ Schema saved to " + schemaPath.toAbsolutePath());
        
    }
}
