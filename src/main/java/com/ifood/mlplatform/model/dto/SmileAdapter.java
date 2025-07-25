package com.ifood.mlplatform.model.dto;

import smile.classification.Classifier;
import smile.data.Tuple;
import smile.data.type.StructType;
import smile.regression.Regression;

import java.io.Serializable;

import com.ifood.mlplatform.model.Predictable;

public class SmileAdapter implements Predictable {

    private final Serializable model;
    private final StructType schema;

    public SmileAdapter(Serializable model, StructType schema) {
        this.model = model;
        this.schema = schema;
    }

    @Override
    public Object predict(double[] features) {
        // Cria Tuple com o schema necessário
        Tuple tuple = Tuple.of(features, schema);

        if (model instanceof Classifier) {
            @SuppressWarnings("unchecked")
            Classifier<Tuple> classifier = (Classifier<Tuple>) model;
            return classifier.predict(tuple);
        } else if (model instanceof Regression) {
            @SuppressWarnings("unchecked")
            Regression<Tuple> regression = (Regression<Tuple>) model;
            return regression.predict(tuple);
        } else {
            throw new UnsupportedOperationException("Unsupported Smile model type: " + model.getClass());
        }
    }
}
