package com.parser.analysis.detectors;

import com.parser.analysis.MetricsDetector;
import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;
import java.util.stream.*;

public class PageRankDetector extends BaseDetector implements MetricsDetector {

    @Override
    public Map<String, Double> detectMetrics(GraphData graphData, Map<String, Object> params) {
        List<String> allowedEdgeTypes = getListStringParam(params, "allowed_edge_types");
        Graph<String, DefaultEdge> targetGraph;

        if (allowedEdgeTypes != null && !allowedEdgeTypes.isEmpty()) {
            targetGraph = buildFilteredGraph(graphData, allowedEdgeTypes);
        } else {
            targetGraph = graphData.getGraph();
        }
        
        if (targetGraph.vertexSet().isEmpty()) {
            return Collections.emptyMap(); // Return empty map for no nodes or no qualifying nodes
        }

        // PageRank algorithm expects a directed graph. 
        // buildFilteredGraph should preserve directedness from GraphData's graph.
        // If targetGraph is somehow not directed, PageRank might error or behave unexpectedly.
        // We assume GraphData.getGraph() is always a DirectedGraph instance (e.g., DirectedMultigraph).
        if (!targetGraph.getType().isDirected()) {
            // This case should ideally not be hit if GraphData consistently provides a directed graph
            // and buildFilteredGraph correctly preserves that type.
            System.err.println("Warning: PageRankDetector received an undirected graph. Results may be incorrect.");
            // Optionally, could throw an error or attempt to convert it, but for now, proceed with caution.
        }

        double threshold = getDoubleParam(params, "threshold", 0.1);
        PageRank<String, DefaultEdge> pr = new PageRank<>(targetGraph);

        return targetGraph.vertexSet().stream()
            .map(v -> new AbstractMap.SimpleEntry<>(v, pr.getVertexScore(v)))
            .filter(entry -> entry.getValue() != null && entry.getValue() > threshold)
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
            ));
    }

    // Removed local buildFilteredGraph, will use BaseDetector's version
}
