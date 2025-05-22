package com.parser.analysis.detectors;

import com.parser.analysis.MetricsDetector;
import com.parser.model.GraphData;
import org.jgrapht.Graph;
import java.util.*;

public class ConnectivityDetector implements MetricsDetector {
    @Override
    public Map<String, Double> detectMetrics(GraphData graphData, Map<String, Object> params) {
        int n = graphData.getGraph().vertexSet().size();
        if (n < 2) {
            return Map.of("connectivity", 0.0);
        }
        int possible = n * (n - 1);
        if (possible == 0) {
             return Map.of("connectivity", 0.0);
        }
        double connectivity = (double) graphData.getGraph().edgeSet().size() / possible;
        
        return Map.of("connectivity", connectivity);
    }
}
