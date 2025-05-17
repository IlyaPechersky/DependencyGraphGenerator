package com.parser.config;

import java.util.Map;

public class CheckConfig {
    private String type;
    private String description;
    private String topology;
    private Map<String, Object> params;

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getTopology() {
        return topology;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
