package com.ifood.mlplatform.model.dto;

import com.ifood.mlplatform.model.Predictable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import smile.classification.Classifier;
import smile.data.Tuple;
import smile.data.type.StructType;
import smile.regression.Regression;

import java.io.Serializable;

@RequiredArgsConstructor
@Getter
public class SmileAdapter implements Predictable, Serializable {

    private static final long serialVersionUID = 1L;

    private final Serializable model;
    private final StructType schema;

    public Object predict(Tuple tuple) {
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
    public Object predict(double[] features) {
        if (schema == null) {
            throw new IllegalStateException("Schema is not available to create Tuple from features.");
        }
        Tuple tuple = Tuple.of(features, schema);
        return predict(tuple);
    }
}
