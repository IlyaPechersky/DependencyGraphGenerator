package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;
import java.util.stream.Collectors; 

public class BambooDetector extends BaseDetector implements TopologyDetector {

    @Override
    public Map<String, List<List<String>>> detect(GraphData graphData, Map<String, Object> params) {
        Graph<String, DefaultEdge> graph = graphData.getGraph();
        int lengthThreshold = getIntParam(params, "max_length", 3);
        int maxDepth = getIntParam(params, "max_depth", 20);
        
        Map<String, List<List<String>>> chains = new LinkedHashMap<>();
        Set<String> structuralSignatures = new HashSet<>();

        for (String vertex : graph.vertexSet()) {
            Deque<List<String>> currentChain = new ArrayDeque<>();
            Set<String> visitedEdges = new HashSet<>();
            findChains(vertex, currentChain, chains, structuralSignatures, lengthThreshold, 
                      maxDepth, graph, graphData, visitedEdges);
        }

        return chains;
    }

    private void findChains(String current,
                           Deque<List<String>> currentChain,
                           Map<String, List<List<String>>> chains,
                           Set<String> structuralSignatures,
                           int lengthThreshold,
                           int maxDepth,
                           Graph<String, DefaultEdge> graph,
                           GraphData graphData,
                           Set<String> visitedEdges) {
        
        if (currentChain.size() >= maxDepth) return;

        for (DefaultEdge edge : graph.outgoingEdgesOf(current)) {
            String source = graph.getEdgeSource(edge);
            String target = graph.getEdgeTarget(edge);
            String edgeType = graphData.getEdgeTypes().getOrDefault(edge, "unknown");
            String edgeKey = source + "->" + target + ":" + edgeType;

            if (visitedEdges.contains(edgeKey)) continue;

            String normalizedTarget = target.replaceAll("(List|Set|Map)<.*>", "Collection<*>");
            List<String> edgeInfo = Arrays.asList(source, normalizedTarget, edgeType);
            
            currentChain.addLast(edgeInfo);
            visitedEdges.add(edgeKey);

            if (currentChain.size() > lengthThreshold) {
                String structuralKey = generateStructuralKey(currentChain);
                if (!structuralSignatures.contains(structuralKey)) {
                    structuralSignatures.add(structuralKey);
                    String chainKey = "Chain-" + structuralKey.hashCode();
                    chains.put(chainKey, new ArrayList<>(currentChain));
                }
            }

            findChains(target, currentChain, chains, structuralSignatures, 
                      lengthThreshold, maxDepth, graph, graphData, visitedEdges);
            
            currentChain.removeLast();
            visitedEdges.remove(edgeKey);
        }
    }

    private String generateStructuralKey(Deque<List<String>> chain) {
        return chain.stream()
            .map(edge -> edge.get(0) + "->" + edge.get(1))
            .sorted()
            .collect(Collectors.joining("|"));
    }
}
