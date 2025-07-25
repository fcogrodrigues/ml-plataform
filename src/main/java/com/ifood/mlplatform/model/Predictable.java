package com.ifood.mlplatform.model;

public interface Predictable {
    Object predict(double[] features);
}