package com.ifood.mlplatform.model.metadata;

import java.util.List;

public class ModelMetadata {
    public String model_type;
    public String framework;
    public List<Feature> features;
    public Label label;

    public static class Feature {
        public String name;
        public String type; // "double", "categorical"
        public List<String> categories; // only for categorical
    }

    public static class Label {
        public String name;
        public String type; // e.g., "integer", "string"
        public List<String> classes; // optional for classification
    }
}