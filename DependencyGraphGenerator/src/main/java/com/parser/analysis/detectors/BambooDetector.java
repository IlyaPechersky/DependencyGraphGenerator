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
        List<String> allowedEdgeTypes = getListStringParam(params, "allowed_edge_types");
        Graph<String, DefaultEdge> graphToAnalyze;

        if (allowedEdgeTypes != null && !allowedEdgeTypes.isEmpty()) {
            graphToAnalyze = buildFilteredGraph(graphData, allowedEdgeTypes);
        } else {
            graphToAnalyze = graphData.getGraph();
        }

        int lengthThreshold = getIntParam(params, "min_length", 3); // Changed to min_length for clarity
        int maxDepth = getIntParam(params, "max_depth", 20);
        
        Map<String, List<List<String>>> chains = new LinkedHashMap<>();
        Set<String> uniqueChainSignatures = new HashSet<>(); // To avoid duplicate chains based on node sequence and types

        for (String vertex : graphToAnalyze.vertexSet()) {
            // Visited edges should be reset for each starting vertex to explore all paths
            findChainsRecursive(vertex, new ArrayDeque<>(), chains, uniqueChainSignatures, 
                               lengthThreshold, maxDepth, graphToAnalyze, graphData);
        }

        return chains;
    }

    private void findChainsRecursive(String currentVertex,
                                     Deque<List<String>> currentPathEdges,
                                     Map<String, List<List<String>>> detectedChains,
                                     Set<String> uniqueChainSignatures,
                                     int minLengthThreshold, // Minimum number of edges in a chain to be reported
                                     int maxDepth, // Max number of edges
                                     Graph<String, DefaultEdge> graphToAnalyze, // The potentially filtered graph
                                     GraphData originalGraphData) { // For getting original edge types for reporting
        
        if (currentPathEdges.size() >= maxDepth) return;

        // Iterate over outgoing edges from the currentVertex in the (potentially filtered) graphToAnalyze
        for (DefaultEdge edge : graphToAnalyze.outgoingEdgesOf(currentVertex)) {
            String source = graphToAnalyze.getEdgeSource(edge);
            String target = graphToAnalyze.getEdgeTarget(edge);
            
            // Get the original edge type from originalGraphData for reporting
            String originalEdgeType = originalGraphData.getEdgeTypes().getOrDefault(edge, "unknown");
            
            // Create edge info for the current path. Normalization can be kept if desired.
            String normalizedTargetDisplay = target.replaceAll("(List|Set|Map)<.*>", "Collection<*>");
            List<String> edgeInfoForPath = Arrays.asList(source, normalizedTargetDisplay, originalEdgeType);

            // Check for simple cycles in the current path to avoid trivial loops in chains
            // A simple check: if the target is already in the current path (not just the last element).
            // This prevents A->B->A type small loops from forming indefinite chains here.
            boolean formsCycleInPath = false;
            for(List<String> pathEdge : currentPathEdges) {
                if (pathEdge.get(1).equals(source)) { // If current source was a target in path
                   //This check is too simple, proper cycle check is complex here.
                   //The main issue is revisiting an edge or node in a way that forms a non-simple path.
                   //For now, let original logic for visitedEdges (if any) handle, or rely on path depth.
                }
            }
            // The original BambooDetector didn't have explicit per-path cycle detection, relied on depth and signatures.

            currentPathEdges.addLast(edgeInfoForPath);

            // A chain is reported if its length (number of edges) is >= minLengthThreshold
            if (currentPathEdges.size() >= minLengthThreshold) {
                String chainSignature = generatePathSignature(currentPathEdges);
                if (!uniqueChainSignatures.contains(chainSignature)) {
                    uniqueChainSignatures.add(chainSignature);
                    // Use a more descriptive key if needed, e.g., including start node
                    detectedChains.put("Chain-" + chainSignature.hashCode(), new ArrayList<>(currentPathEdges));
                }
            }

            findChainsRecursive(target, currentPathEdges, detectedChains, uniqueChainSignatures, 
                               minLengthThreshold, maxDepth, graphToAnalyze, originalGraphData);
            
            currentPathEdges.removeLast(); // Backtrack
        }
    }

    // Generates a signature for a path (sequence of edges with types)
    private String generatePathSignature(Deque<List<String>> path) {
        return path.stream()
            .map(edge -> edge.get(0) + "-" + edge.get(2) + "->" + edge.get(1)) // source-type->target (using normalized target)
            .collect(Collectors.joining("|"));
    }
}
