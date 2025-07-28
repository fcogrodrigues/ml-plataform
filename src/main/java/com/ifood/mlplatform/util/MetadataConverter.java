package com.ifood.mlplatform.util;

import com.ifood.mlplatform.model.metadata.ModelMetadata;
import smile.data.type.DataType;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;

import java.util.ArrayList;
import java.util.List;

public class MetadataConverter {
    public static StructType toInputSchema(ModelMetadata metadata) {
        List<StructField> fields = new ArrayList<>();

        String labelName = metadata.label.name;
        for (ModelMetadata.Feature feature : metadata.features) {
            if (feature.name.equals(labelName)) {
                continue;  
            }
            DataType type = mapType(feature.type);
            fields.add(new StructField(feature.name, type));
        }

        return new StructType(fields);
    }

    public static StructType toFullSchema(ModelMetadata metadata) {
        List<StructField> fields = new ArrayList<>();
        // all features
        for (ModelMetadata.Feature f : metadata.features) {
            fields.add(new StructField(f.name, mapType(f.type)));
        }
        // add label at final
        fields.add(new StructField(metadata.label.name,
                                mapType(metadata.label.type)));
        return new StructType(fields);
    }

    private static DataType mapType(String type) {
        return switch (type.toLowerCase()) {
            case "double"  -> DataTypes.DoubleType;
            case "integer" -> DataTypes.IntegerType;
            case "string"  -> DataTypes.StringType;
            case "boolean" -> DataTypes.BooleanType;
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
    }
}