package com.parser.analysis.detectors;

import java.util.Map;

public abstract class BaseDetector {
    
    protected double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (!params.containsKey(key)) return defaultValue;
        Object param = params.get(key);
        
        if (param instanceof Map) {
            Map<String, Object> nestedParam = (Map<String, Object>) param;
            return ((Number) nestedParam.getOrDefault("value", defaultValue)).doubleValue();
        }
        return ((Number) param).doubleValue();
    }

    protected int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (!params.containsKey(key)) return defaultValue;
        Object param = params.get(key);
        
        if (param instanceof Map) {
            Map<String, Object> nestedParam = (Map<String, Object>) param;
            return ((Number) nestedParam.getOrDefault("value", defaultValue)).intValue();
        }
        return ((Number) param).intValue();
    }
}
