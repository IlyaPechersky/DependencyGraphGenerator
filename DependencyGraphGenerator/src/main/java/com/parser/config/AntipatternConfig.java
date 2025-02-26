package com.parser.config;

import java.util.List;

public class AntipatternConfig {
    private String name;
    private List<CheckConfig> checks;

    public String getName() {
        return name;
    }

    public List<CheckConfig> getChecks() {
        return checks;
    }
}
