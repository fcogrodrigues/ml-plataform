package com.ifood.mlplatform.util;

import com.ifood.mlplatform.model.metadata.ModelMetadata;
import smile.data.type.DataType;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;

import java.util.ArrayList;
import java.util.List;

public class MetadataConverter {

    public static StructType toStructType(ModelMetadata metadata) {
        List<StructField> fields = new ArrayList<>();

        for (ModelMetadata.Feature feature : metadata.features) {
            DataType dataType = mapType(feature.type);
            fields.add(new StructField(feature.name, dataType));
        }

        return new StructType(fields);
    }

    private static DataType mapType(String type) {
        switch (type.toLowerCase()) {
            case "double":
                return DataTypes.DoubleType;
            case "integer":
                return DataTypes.IntegerType;
            case "string":
                return DataTypes.StringType;
            case "boolean":
                return DataTypes.BooleanType;
            default:
                throw new IllegalArgumentException("❌ Unsupported feature type: " + type);
        }
    }
}
