package com.ifood.mlplatform.model.dto;

import com.ifood.mlplatform.model.ModelAdapter;
import com.ifood.mlplatform.model.Predictable;
import com.ifood.mlplatform.model.metadata.ModelMetadata;
import com.ifood.mlplatform.util.MetadataConverter;

import lombok.RequiredArgsConstructor;
import smile.classification.Classifier;
import smile.data.Tuple;
import smile.data.type.StructType;
import smile.regression.Regression;

import java.io.Serializable;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class SmileAdapter implements ModelAdapter {

    @Override
    public boolean supports(ModelMetadata md) {
        return "SMILE".equalsIgnoreCase(md.framework);
    }

    @Override
    public Predictable load(Serializable rawModel, ModelMetadata md) {
        // 1) schema completo (features + label) para criar o Tuple
        StructType fullSchema = MetadataConverter.toFullSchema(md);
        // 2) número de colunas que são *features* (sem o label)
        int featureCount = md.features.size();
        // 3) nomes das classes para tradução
        String[] classes = md.label.classes.toArray(new String[0]);
        return new SmilePredictor(rawModel, fullSchema, classes, featureCount);
    }

    @RequiredArgsConstructor
    private static class SmilePredictor implements Predictable, Serializable {
        private final Serializable model;
        private final StructType  schema;
        private final String[]    classes;
        private final int         featureCount;

        @Override
        public Object predict(Map<String, Object> features) {
            // 1) cria o vetor completo: features + 1 slot de dummy label
            double[] row = new double[schema.length()];

            // 2) percorre apenas as colunas de input
            for (int i = 0; i < featureCount; i++) {
                String name = schema.field(i).name;
                Object v = features.get(name);
                if (v == null) {
                    throw new IllegalArgumentException("Missing feature: " + name);
                }
                try {
                    row[i] = (v instanceof Number)
                           ? ((Number) v).doubleValue()
                           : Double.parseDouble(v.toString());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid value for feature: " + name, e);
                }
            }

            // 3) coloca dummy no slot de label
            row[featureCount] = 0;

            // 4) monta o Tuple e chama SMILE
            Tuple t = Tuple.of(row, schema);
            Object raw;
            if (model instanceof Classifier) {
                @SuppressWarnings("unchecked")
                Classifier<Tuple> clf = (Classifier<Tuple>) model;
                raw = clf.predict(t);
                // traduz índice → rótulo
                int idx = (Integer) raw;
                return (idx >= 0 && idx < classes.length)
                     ? classes[idx]
                     : idx;
            }
            else if (model instanceof Regression) {
                @SuppressWarnings("unchecked")
                Regression<Tuple> reg = (Regression<Tuple>) model;
                raw = reg.predict(t);
                return raw;
            }
            else {
                throw new UnsupportedOperationException(
                    "Unsupported model type: " + model.getClass().getName()
                );
            }
        }
    }
}
