package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.AsSubgraph;
import java.util.*;

public class TreeDetector extends BaseDetector implements TopologyDetector {

    @Override
    public Map<String, List<List<String>>> detect(GraphData graphData, Map<String, Object> params) {
        List<String> allowedEdgeTypes = getListStringParam(params, "allowed_edge_types");
        Graph<String, DefaultEdge> graphToAnalyze;

        if (allowedEdgeTypes != null && !allowedEdgeTypes.isEmpty()) {
            graphToAnalyze = buildFilteredGraph(graphData, allowedEdgeTypes);
        } else {
            graphToAnalyze = graphData.getGraph();
        }

        int minNodes = getIntParam(params, "min_nodes", 3);
        Map<String, List<List<String>>> detectedTrees = new LinkedHashMap<>();

        ConnectivityInspector<String, DefaultEdge> inspector = new ConnectivityInspector<>(graphToAnalyze);
        List<Set<String>> connectedComponents = inspector.connectedSets();
        int treeNumber = 1;

        for (Set<String> componentNodes : connectedComponents) {
            if (componentNodes.size() < minNodes) continue;

            Graph<String, DefaultEdge> componentGraph = new AsSubgraph<>(graphToAnalyze, componentNodes);
            
            org.jgrapht.alg.cycle.CycleDetector<String, DefaultEdge> componentCycleDetector = 
                new org.jgrapht.alg.cycle.CycleDetector<>(componentGraph);
            
            if (!componentCycleDetector.detectCycles()) {
                if (componentGraph.edgeSet().size() == componentNodes.size() - 1 || componentNodes.size() == 1 && componentGraph.edgeSet().isEmpty()) { 
                    List<List<String>> treeEdgesReport = new ArrayList<>();
                    for (DefaultEdge edge : componentGraph.edgeSet()) {
                        String source = componentGraph.getEdgeSource(edge);
                        String target = componentGraph.getEdgeTarget(edge);
                        String originalEdgeType = graphData.getEdgeTypes().getOrDefault(edge, "unknown");
                        treeEdgesReport.add(Arrays.asList(source, target, originalEdgeType));
                    }
                    if (!treeEdgesReport.isEmpty() || componentNodes.size() >= minNodes && componentNodes.size() == 1) {
                        String rootCandidate = componentNodes.stream().findFirst().orElse("UnknownRoot");
                        detectedTrees.put("Tree-" + treeNumber++ + " (Rootish: " + rootCandidate + ")", treeEdgesReport);
                    }
                }
            }
        }
        return detectedTrees;
    }
}
