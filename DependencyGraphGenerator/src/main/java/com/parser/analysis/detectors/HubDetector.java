package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;
import java.util.stream.*;


public class HubDetector extends BaseDetector implements TopologyDetector {
    @Override
    public Map<String, Object> detect(Graph<String, DefaultEdge> graph, Map<String, Object> params) {
        int max_degree = getIntParam(params, "max_degree", 10);
        Map<String, Integer> hubs = new HashMap<>();

        for (String vertex : graph.vertexSet()) {
            int degree = graph.inDegreeOf(vertex) + graph.outDegreeOf(vertex);
            if (degree >= max_degree) {
                hubs.put(vertex, degree);
            }
        }

        return hubs.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }
}
