package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import com.parser.model.GraphData;
import org.jgrapht.alg.clique.BronKerboschCliqueFinder;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.AsUndirectedGraph;
import java.util.*;

public class CliqueDetector extends BaseDetector implements TopologyDetector {
    private static final int MAX_CLIQUE_SIZE_DEFAULT = 6;
    private static final int MAX_REPORTED_CLIQUES_DEFAULT = 100;
    
    @Override
    public Map<String, List<List<String>>> detect(GraphData graphData, Map<String, Object> params) {
        List<String> allowedEdgeTypes = getListStringParam(params, "allowed_edge_types");
        Graph<String, DefaultEdge> graphToAnalyze;

        if (allowedEdgeTypes != null && !allowedEdgeTypes.isEmpty()) {
            graphToAnalyze = buildFilteredGraph(graphData, allowedEdgeTypes);
        } else {
            graphToAnalyze = graphData.getGraph();
        }

        int minSize = getIntParam(params, "min_size", 3);
        int maxSize = getIntParam(params, "max_size", MAX_CLIQUE_SIZE_DEFAULT);
        int maxReportedCliques = getIntParam(params, "max_reported_cliques", MAX_REPORTED_CLIQUES_DEFAULT);

        Graph<String, DefaultEdge> undirectedView = graphToAnalyze.getType().isDirected() ? 
                                                    new AsUndirectedGraph<>(graphToAnalyze) : 
                                                    graphToAnalyze;
        
        BronKerboschCliqueFinder<String, DefaultEdge> finder = new BronKerboschCliqueFinder<>(undirectedView);
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        int cliqueNumber = 1;
        
        for (Set<String> clique : finder) {
            if (clique.size() >= minSize && clique.size() <= maxSize) {
                List<List<String>> edges = convertCliqueToEdgesWithTypes(clique, graphToAnalyze, graphData);
                if (!edges.isEmpty()) {
                    result.put("Clique " + cliqueNumber++, edges);
                }
                if (cliqueNumber > maxReportedCliques) break; 
            }
        }
        return result;
    }
    
    private List<List<String>> convertCliqueToEdgesWithTypes(Set<String> cliqueNodes, Graph<String, DefaultEdge> sourceGraph, GraphData originalGraphData) {
        List<List<String>> edgesList = new ArrayList<>();
        List<String> nodes = new ArrayList<>(cliqueNodes);
        Map<DefaultEdge, String> allOriginalEdgeTypes = originalGraphData.getEdgeTypes();
        
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                String u = nodes.get(i);
                String v = nodes.get(j);

                Set<DefaultEdge> edgesUV = sourceGraph.getAllEdges(u, v);
                if (edgesUV != null) {
                    for (DefaultEdge edge : edgesUV) {
                        String type = allOriginalEdgeTypes.getOrDefault(edge, "unknown");
                        edgesList.add(Arrays.asList(u, v, type));
                    }
                }
                
                Set<DefaultEdge> edgesVU = sourceGraph.getAllEdges(v, u);
                if (edgesVU != null) {
                    for (DefaultEdge edge : edgesVU) {
                        String type = allOriginalEdgeTypes.getOrDefault(edge, "unknown");
                        edgesList.add(Arrays.asList(v, u, type));
                    }
                }
            }
        }
        return edgesList;
    }
}
