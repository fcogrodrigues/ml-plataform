package com.ifood.mlplatform.model.dto;

import com.ifood.mlplatform.model.Predictable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import smile.classification.Classifier;
import smile.data.Tuple;
import smile.data.type.StructType;
import smile.regression.Regression;

import java.io.Serializable;
import java.util.Map;

@RequiredArgsConstructor
@Getter
public class SmileAdapter implements Predictable, Serializable {

    private static final long serialVersionUID = 1L;

    private final Serializable model;
    private final StructType schema;

    private Object predict(Tuple tuple) {
        if (model instanceof Classifier) {
            @SuppressWarnings("unchecked")
            Classifier<Tuple> classifier = (Classifier<Tuple>) model;
            return classifier.predict(tuple);
        } else if (model instanceof Regression) {
            Regression<Tuple> regression = (Regression<Tuple>) model;
            return regression.predict(tuple);
        } else {
            throw new UnsupportedOperationException(
                "Unsupported Smile model type: " + model.getClass().getName()
            );
        }
    }

    @Override
    public Object predict(Map<String, Object> features) {
        double[] featureArray = new double[schema.length()];

        for (int i = 0; i < schema.length(); i++) {
            String name = schema.field(i).name;
            Object value = features.get(name);

            if (value == null) {
                throw new IllegalArgumentException("Missing feature: " + name);
            }

            try {
                featureArray[i] = (value instanceof Number)
                        ? ((Number) value).doubleValue()
                        : Double.parseDouble(value.toString());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid value for feature: " + name, e);
            }
        }

        Tuple tuple = Tuple.of(featureArray, schema);
        return predict(tuple);
    }
}
