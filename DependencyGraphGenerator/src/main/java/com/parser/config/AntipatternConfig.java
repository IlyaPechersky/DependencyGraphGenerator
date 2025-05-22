package com.parser.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class AntiPatternConfig {
    private Map<String, AntiPatternDefinition> antiPatterns;

    @JsonProperty("anti_patterns")
    public Map<String, AntiPatternDefinition> getAntiPatterns() {
        return antiPatterns;
    }

    public void setAntiPatterns(Map<String, AntiPatternDefinition> antiPatterns) {
        this.antiPatterns = antiPatterns;
    }

    public static class AntiPatternDefinition {
        private String description;
        private RuleConditions conditions;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public RuleConditions getConditions() {
            return conditions;
        }

        public void setConditions(RuleConditions conditions) {
            this.conditions = conditions;
        }
    }
}
