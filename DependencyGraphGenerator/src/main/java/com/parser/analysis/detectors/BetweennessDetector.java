package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;


public class BetweennessDetector extends BaseDetector implements TopologyDetector {
    @Override
    public Map<String, Double> detect(Graph<String, DefaultEdge> graph, Map<String, Object> params) {
        // double threshold = (double) params.getOrDefault("threshold", 0.1);
        double threshold = 0.15;
        BetweennessCentrality<String, DefaultEdge> bc = new BetweennessCentrality<>(graph);
        
        double max = graph.vertexSet().stream()
            .mapToDouble(bc::getVertexScore)
            .max().orElse(1.0);

        Map<String, Double> results = new LinkedHashMap<>();
        graph.vertexSet().stream()
            .filter(v -> bc.getVertexScore(v)/max > threshold)
            .sorted((a, b) -> Double.compare(
                bc.getVertexScore(b)/max, 
                bc.getVertexScore(a)/max
            ))
            .forEach(v -> results.put(v, bc.getVertexScore(v)/max));
        
        return results;
    }
}
