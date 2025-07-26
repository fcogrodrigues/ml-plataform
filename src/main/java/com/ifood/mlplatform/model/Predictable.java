package com.ifood.mlplatform.model;
import java.util.Map;

public interface Predictable {
    Object predict(Map<String, Object> features);
}