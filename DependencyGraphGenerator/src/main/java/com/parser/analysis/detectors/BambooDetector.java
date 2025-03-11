package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;


public class BambooDetector extends BaseDetector implements TopologyDetector {
    @Override
    public Map<String, List<String>> detect(Graph<String, DefaultEdge> graph, Map<String, Object> params) {
        int maxLength = getIntParam(params, "max_length", 5);
        
        Map<String, List<String>> chains = new HashMap<>();

        for (String vertex : graph.vertexSet()) {
            List<String> chain = new ArrayList<>();
            findChains(vertex, chain, chains, maxLength, graph);
        }

        return chains;
    }

    private void findChains(String current, List<String> chain, Map<String, List<String>> chains, int maxLength, Graph<String, DefaultEdge> graph) {
        chain.add(current);

        if (chain.size() > maxLength) {
            chains.put(current, new ArrayList<>(chain));
            return;
        }

        for (DefaultEdge edge : graph.outgoingEdgesOf(current)) {
            String target = graph.getEdgeTarget(edge);
            findChains(target, chain, chains, maxLength, graph);
        }

        chain.remove(chain.size() - 1);
    }
}
