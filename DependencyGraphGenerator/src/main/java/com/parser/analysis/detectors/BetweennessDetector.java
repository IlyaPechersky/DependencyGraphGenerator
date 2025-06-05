package com.parser.analysis.detectors;

import com.parser.analysis.MetricsDetector;
import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;

public class BetweennessDetector extends BaseDetector implements MetricsDetector {
    @Override
    public Map<String, Double> detectMetrics(GraphData graphData, Map<String, Object> params) {
        List<String> allowedEdgeTypes = getListStringParam(params, "allowed_edge_types");
        Graph<String, DefaultEdge> targetGraph;

        if (allowedEdgeTypes != null && !allowedEdgeTypes.isEmpty()) {
            targetGraph = buildFilteredGraph(graphData, allowedEdgeTypes);
        } else {
            targetGraph = graphData.getGraph();
        }

        if (targetGraph.vertexSet().isEmpty()) {
            return Collections.emptyMap();
        }

        final double threshold = getDoubleParam(params, "threshold", 0.15);

        final BetweennessCentrality<String, DefaultEdge> bc = new BetweennessCentrality<>(targetGraph, true);
        
        double maxScoreValue = targetGraph.vertexSet().stream()
            .mapToDouble(bc::getVertexScore)
            .max().orElse(1.0);
        
        if (maxScoreValue == 0) maxScoreValue = 1.0;
        final double finalMaxScore = maxScoreValue;

        Map<String, Double> results = new LinkedHashMap<>();
        targetGraph.vertexSet().stream()
            .map(v -> new AbstractMap.SimpleEntry<>(v, bc.getVertexScore(v) / finalMaxScore))
            .filter(entry -> entry.getValue() > threshold)
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .forEach(entry -> results.put(entry.getKey(), entry.getValue()));
        
        return results;
    }
}
