package com.parser.analysis.detectors;

import java.util.Map;

public abstract class BaseDetector {
    
    protected double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object param = params.get(key);
        
        if (param instanceof Map) {
            Map<String, Object> nestedParam = (Map<String, Object>) param;
            return ((Number) nestedParam.getOrDefault("value", defaultValue)).doubleValue();
        }
        return ((Number) param).doubleValue();
    }

    protected int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object param = params.get(key);
        
        if (param instanceof Map) {
            Map<String, Object> nestedParam = (Map<String, Object>) param;
            return ((Number) nestedParam.getOrDefault("value", defaultValue)).intValue();
        }
        return ((Number) param).intValue();
    }

    protected boolean getBoolParam(Map<String, Object> params, String key, boolean defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        return defaultValue;
    }

    protected String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        return (value != null) ? value.toString() : defaultValue;
    }
}
