package com.parser.analysis;

import com.parser.analysis.TopologyDetector;
import com.parser.analysis.MetricsDetector;
import com.parser.analysis.DetectorFactory;
import com.parser.config.AntiPatternConfig.AntiPatternDefinition;
import com.parser.config.RuleConditions;
import com.parser.config.Rule;
import com.parser.model.GraphData;
import java.util.stream.Collectors;
import java.util.*;

public class ConfigurableDetector implements TopologyDetector, MetricsDetector {
    private final AntiPatternDefinition definition;

    public ConfigurableDetector(AntiPatternDefinition definition) {
        this.definition = definition;
    }

    @Override
    public Map<String, List<List<String>>> detect(GraphData graphData, Map<String, Object> params) {
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        
        if (evaluateConditions(graphData, definition.getConditions())) {
            List<TopologyDetector> detectors = resolveDetectors(definition.getConditions());
            detectors.forEach(detector -> {
                Map<String, List<List<String>>> findings = detector.detect(graphData, params);
                findings.forEach((key, edges) -> {
                    result.computeIfAbsent(key, k -> new ArrayList<>()).addAll(edges);
                });
            });
        }
        
        return result;
    }

    @Override
    public Map<String, Double> detectMetrics(GraphData graphData, Map<String, Object> params) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        
        definition.getConditions().getRules().stream()
            .filter(rule -> "metric".equals(rule.getType()))
            .forEach(rule -> {
                MetricsDetector detector = DetectorFactory.getMetricDetector(rule.getMetric());
                if (detector != null) {
                    Map<String, Double> values = detector.detectMetrics(graphData, rule.getParams());
                    metrics.putAll(values);
                }
            });
        
        return metrics;
    }

    private Map<String, List<List<String>>> collectFindings(GraphData graphData) {
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        
        List<TopologyDetector> topologyDetectors = resolveDetectors(definition.getConditions());
        
        for (TopologyDetector detector : topologyDetectors) {
            Map<String, List<List<String>>> findings = detector.detect(graphData, null);
            findings.forEach((key, edges) -> {
                result.computeIfAbsent(definition.getDescription(), k -> new ArrayList<>())
                    .addAll(edges);
            });
        }
        
        return result;
    }

    private List<TopologyDetector> resolveDetectors(RuleConditions conditions) {
        return conditions.getRules().stream()
            .filter(rule -> "topology".equals(rule.getType()))
            .map(rule -> DetectorFactory.getTopologyDetector(rule.getDetector()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private boolean evaluateConditions(GraphData graphData, RuleConditions conditions) {
        boolean result = true;
        String operator = conditions.getOperator().toUpperCase();
        
        for (Rule rule : conditions.getRules()) {
            boolean currentResult;
            
            if ("topology".equals(rule.getType())) {
                Map<String, List<List<String>>> findings = checkDetectorRule(graphData, rule);
                currentResult = !findings.isEmpty();
            } 
            else if ("metric".equals(rule.getType())) {
                currentResult = checkMetricRule(graphData, rule);
            } 
            else {
                throw new IllegalArgumentException("Unknown rule type: " + rule.getType());
            }
            
            if ("AND".equals(operator)) {
                result = result && currentResult;
            } else if ("OR".equals(operator)) {
                result = result || currentResult;
            }
        }
        
        return result;
    }

    private Set<String> extractNodes(Map<String, List<List<String>>> findings) {
        return findings.values().stream()
            .flatMap(List::stream)
            .flatMap(List::stream)
            .filter(node -> !node.matches(".*->.*"))
            .collect(Collectors.toSet());
    }

    private boolean hasOverlap(List<Set<String>> nodeSets) {
        return nodeSets.stream()
            .reduce((s1, s2) -> { s1.retainAll(s2); return s1; })
            .map(set -> !set.isEmpty())
            .orElse(false);
    }

    private Map<String, List<List<String>>> checkDetectorRule(GraphData graphData, Rule rule) {
        String detectorName = rule.getDetector();
        if (detectorName == null) {
            System.err.println("Rule has no detector specified");
            return Collections.emptyMap();
        }
        
        TopologyDetector detector = DetectorFactory.getTopologyDetector(rule.getDetector());
        if (detector == null) return Collections.emptyMap();
        
        Map<String, List<List<String>>> findings = detector.detect(
            graphData, 
            rule.getParams() != null ? rule.getParams() : Collections.emptyMap()
        );
        
        boolean countCondition = rule.getMinCount() == null || 
                            findings.size() >= rule.getMinCount();
        
        boolean nodeCondition = true;
        if (rule.getContainsNode() != null) {
            nodeCondition = findings.values().stream()
                .flatMap(List::stream)
                .anyMatch(edge -> edge.contains(rule.getContainsNode()));
        }
        
        return countCondition && nodeCondition ? findings : Collections.emptyMap();
    }

    private boolean checkMetricRule(GraphData graphData, Rule rule) {
        String metricName = rule.getMetric();
        if (metricName == null) {
            System.err.println("Metric name is not specified");
            return false;
        }

        MetricsDetector detector = DetectorFactory.getMetricDetector(metricName);
        if (detector == null) {
            System.err.println("Metric detector not found: " + metricName);
            return false;
        }

        Map<String, Object> params = rule.getParams() != null ? 
            rule.getParams() : Collections.emptyMap();

        try {
            Map<String, Double> metrics = detector.detectMetrics(graphData, params);
            Double value = metrics.get(metricName);
            
            if (value == null) {
                System.err.println("Metric not calculated: " + metricName);
                return false;
            }

            return checkCondition(value, rule.getOperator(), rule.getValue());
            
        } catch (Exception e) {
            System.err.println("Metric calculation failed: " + e.getMessage());
            return false;
        }
    }

    private boolean hasAny(List<Set<String>> nodeSets) {
        return nodeSets.stream().anyMatch(set -> !set.isEmpty());
    }

    private boolean checkCondition(Double actualValue, String operator, Object expectedValue) {
        if (actualValue == null || operator == null || expectedValue == null) {
            return false;
        }

        double expected;
        try {
            expected = ((Number) expectedValue).doubleValue();
        } catch (ClassCastException e) {
            System.err.println("Invalid metric value type: " + expectedValue.getClass());
            return false;
        }

        switch (operator) {
            case ">": return actualValue > expected;
            case "<": return actualValue < expected;
            case ">=": return actualValue >= expected;
            case "<=": return actualValue <= expected;
            case "==": return Math.abs(actualValue - expected) < 1e-9;
            default: 
                System.err.println("Unsupported operator: " + operator);
                return false;
        }
    }
}
