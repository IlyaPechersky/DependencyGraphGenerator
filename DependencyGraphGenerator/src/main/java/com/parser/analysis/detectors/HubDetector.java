package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;
import java.util.stream.Collectors;

public class HubDetector extends BaseDetector implements TopologyDetector {
    
    @Override
    public Map<String, List<List<String>>> detect(GraphData graphData, Map<String, Object> params) {
        Graph<String, DefaultEdge> graph = graphData.getGraph();
        int maxDegree = getIntParam(params, "max_degree", 10);
        Map<String, List<List<String>>> result = new LinkedHashMap<>();

        List<Map.Entry<String, Integer>> hubs = graph.vertexSet().stream()
            .map(vertex -> Map.entry(
                vertex, 
                graph.inDegreeOf(vertex) + graph.outDegreeOf(vertex)
            ))
            .filter(entry -> entry.getValue() >= maxDegree)
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toList());

        int counter = 1;
        for (Map.Entry<String, Integer> entry : hubs) {
            result.put("Hub " + counter++, Arrays.asList(
                Arrays.asList(
                    entry.getKey(), 
                    String.valueOf(entry.getValue())
                )
            ));
        }

        return result;
    }
}
