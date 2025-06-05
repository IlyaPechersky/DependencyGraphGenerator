package com.parser.analysis;

import java.util.*;

public class AnalysisResult {
    private Map<String, List<List<String>>> topologies = new LinkedHashMap<>();
    private Map<String, Double> metrics = new LinkedHashMap<>();

    public void addTopology(String name, List<List<String>> edges) {
        topologies.put(name, edges);
    }
    
    public void addMetric(String name, Double value) {
        metrics.put(name, value);
    }
}
