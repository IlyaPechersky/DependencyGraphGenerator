package com.parser.analysis.detectors;

import com.parser.analysis.MetricsDetector;
import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;

public class ConnectivityDetector extends BaseDetector implements MetricsDetector {
    @Override
    public Map<String, Double> detectMetrics(GraphData graphData, Map<String, Object> params) {
        List<String> allowedEdgeTypes = getListStringParam(params, "allowed_edge_types");
        Graph<String, DefaultEdge> targetGraph;

        if (allowedEdgeTypes != null && !allowedEdgeTypes.isEmpty()) {
            targetGraph = buildFilteredGraph(graphData, allowedEdgeTypes);
        } else {
            targetGraph = graphData.getGraph(); // Use original if no filter
        }

        int n = targetGraph.vertexSet().size();
        if (n < 2) {
            return Map.of("connectivity", 0.0);
        }
        // For a directed graph, max possible edges is n * (n - 1).
        // For an undirected graph, it's n * (n - 1) / 2.
        // The original GraphData graph is DirectedMultigraph. buildFilteredGraph preserves type.
        int possibleEdges = n * (n - 1);
        if (targetGraph.getType().isUndirected()) { // Should not happen if original is always directed
            possibleEdges /= 2;
        }

        if (possibleEdges == 0 && n >=2 ) { // n < 2 handled, this is for n*(n-1) being 0 for other reasons e.g. n=0,1
             // if n>=2, possibleEdges can't be 0 unless it's an issue with logic or graph state
             // However, if targetGraph.edgeSet().size() is 0, result is 0. If possibleEdges is 0, it's NaN.
             // The n < 2 check should prevent possibleEdges == 0 if graph is valid.
             // If n=0 or n=1, n<2 returns {connectivity:0.0}. This case should be covered.
             // So, if possibleEdges is 0 here, it implies n was 0 or 1, which is already handled.
             // For safety, if it still becomes 0, and edges are also 0, connectivity is 0.
             // if edges > 0 and possibleEdges = 0, it's an issue, but typically means n < 2.
            if(targetGraph.edgeSet().isEmpty()) return Map.of("connectivity", 0.0);
            // else it's an undefined state, but JGraphT might handle division by zero gracefully or error out
            // For now, we assume n < 2 covers the zero denominator for 'possibleEdges'.
        }
        if (possibleEdges == 0) { // Fallback to prevent division by zero if previous checks didn't catch it for n >=2.
            return Map.of("connectivity", 0.0);
        }
        
        double connectivity = (double) targetGraph.edgeSet().size() / possibleEdges;
        
        return Map.of("connectivity", connectivity);
    }
}
