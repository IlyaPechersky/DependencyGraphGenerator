package com.parser.analysis.detectors;

import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import java.util.Map;
import java.util.List;
import java.util.Collections;

public abstract class BaseDetector {
    
    protected double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object param = params.get(key);
        
        if (param instanceof Map) {
            Map<String, Object> nestedParam = (Map<String, Object>) param;
            return ((Number) nestedParam.getOrDefault("value", defaultValue)).doubleValue();
        }
        return ((Number) param).doubleValue();
    }

    protected int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object param = params.get(key);
        
        if (param instanceof Map) {
            Map<String, Object> nestedParam = (Map<String, Object>) param;
            return ((Number) nestedParam.getOrDefault("value", defaultValue)).intValue();
        }
        return ((Number) param).intValue();
    }

    protected boolean getBoolParam(Map<String, Object> params, String key, boolean defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        return defaultValue;
    }

    protected String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object value = params.get(key);
        return (value != null) ? value.toString() : defaultValue;
    }

    @SuppressWarnings("unchecked")
    protected List<String> getListStringParam(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) {
            return Collections.emptyList();
        }
        Object value = params.get(key);
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (!(item instanceof String)) {
                    System.err.println("Warning: Parameter '" + key + "' in rule params contains non-string elements.");
                    return Collections.emptyList(); 
                }
            }
            return (List<String>) value;
        }
        System.err.println("Warning: Parameter '" + key + "' in rule params is not a List.");
        return Collections.emptyList();
    }

    /**
     * Builds a new graph containing only the vertices and edges from the original graph 
     * that match the allowed edge types. If allowedEdgeTypes is null or empty, 
     * a shallow copy of the original graph structure is effectively returned (or the original if immutable).
     * Preserves the directedness of the original graph.
     */
    protected Graph<String, DefaultEdge> buildFilteredGraph(GraphData graphData, List<String> allowedEdgeTypes) {
        Graph<String, DefaultEdge> originalGraph = graphData.getGraph();
        Map<DefaultEdge, String> allEdgeTypesMap = graphData.getEdgeTypes();

        // If no filter is specified, use the original graph directly (or a copy if modification is a concern elsewhere)
        // For safety in JGraphT algorithms that might modify, a new graph is better.
        // We must ensure the new graph has the same type (directed/undirected) as original.
        Graph<String, DefaultEdge> filteredGraph;
        if (originalGraph.getType().isDirected()) {
            // Assuming DirectedMultigraph is the general type used in GraphData
            filteredGraph = new DirectedMultigraph<>(DefaultEdge.class);
        } else {
            // If original can be undirected. Adjust if GraphData always uses a specific type.
            filteredGraph = new SimpleGraph<>(DefaultEdge.class); // Example for simple undirected
        }

        for (String vertex : originalGraph.vertexSet()) {
            filteredGraph.addVertex(vertex);
        }

        if (allowedEdgeTypes == null || allowedEdgeTypes.isEmpty()) {
            // No type filter, so add all edges from original graph
            for (DefaultEdge edge : originalGraph.edgeSet()) {
                String source = originalGraph.getEdgeSource(edge);
                String target = originalGraph.getEdgeTarget(edge);
                filteredGraph.addEdge(source, target, edge); // Add the original edge instance if possible
            }
        } else {
            // Filter by edge type
            for (DefaultEdge edge : originalGraph.edgeSet()) {
                String edgeType = allEdgeTypesMap.get(edge);
                if (edgeType != null && allowedEdgeTypes.contains(edgeType)) {
                    String source = originalGraph.getEdgeSource(edge);
                    String target = originalGraph.getEdgeTarget(edge);
                    filteredGraph.addEdge(source, target, edge); // Add the original edge instance
                }
            }
        }
        return filteredGraph;
    }
}
