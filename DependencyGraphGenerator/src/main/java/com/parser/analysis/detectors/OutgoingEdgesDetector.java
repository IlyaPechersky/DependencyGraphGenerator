package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;
import java.util.stream.*;


public class OutgoingEdgesDetector extends BaseDetector implements TopologyDetector {
    @Override
    public Map<String, Integer> detect(Graph<String, DefaultEdge> graph, Map<String, Object> params) {
        int max_degree = getIntParam(params, "max_out_degree", 20);
        Map<String, Integer> chatty = new HashMap<>();

        for (String vertex : graph.vertexSet()) {
            int outDegree = graph.outDegreeOf(vertex);
            if (outDegree >= max_degree) {
                chatty.put(vertex, outDegree);
            }
        }

        return chatty.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }
}
