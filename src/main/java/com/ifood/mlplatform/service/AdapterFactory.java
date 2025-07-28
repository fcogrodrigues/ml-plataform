package com.ifood.mlplatform.service;

import com.ifood.mlplatform.model.ModelAdapter;
import com.ifood.mlplatform.model.metadata.ModelMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AdapterFactory {

    private final List<ModelAdapter> adapters;
    private final Map<String, ModelAdapter> cache = new ConcurrentHashMap<>();

    public ModelAdapter getAdapter(ModelMetadata metadata) {
        String framework = metadata.framework.toUpperCase(Locale.ROOT);

        return cache.computeIfAbsent(framework, fw -> adapters
                    .stream()
                    .filter(a -> a.supports(metadata))
                    .findFirst()
                    .orElseThrow(() -> 
                        new IllegalStateException("No adapter for framework " + fw)
                    )
        );
    }
}