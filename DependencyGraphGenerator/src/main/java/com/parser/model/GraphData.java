package com.parser.model;

import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphData {
    private final Graph<String, DefaultEdge> graph;
    private final Map<String, String> nodeTypes;
    private final Map<DefaultEdge, String> edgeTypes;
    private final String projectPackagePrefix;

    public GraphData(String projectPackagePrefix) {
        this.projectPackagePrefix = projectPackagePrefix;
        this.graph = new DirectedMultigraph<>(DefaultEdge.class);
        this.nodeTypes = new HashMap<>();
        this.edgeTypes = new HashMap<>();
    }

    public void addNode(String name, String type) {
        if (name == null || name.isEmpty() || name.matches("^(boolean|byte|char|short|int|long|float|double|void)$")) {
            return;
        }
        if (!graph.containsVertex(name)) {
            graph.addVertex(name);
            nodeTypes.put(name, type);
        }
    }

    public void addEdge(String source, String target, String edgeType) {
        if (source.equals(target) || !isProjectClass(source) || !isProjectClass(target)) {
            return;
        }
        if (!graph.containsVertex(source) || !graph.containsVertex(target)) {
            return;
        }
        DefaultEdge edge = graph.addEdge(source, target);
        if (edge != null) {
            edgeTypes.put(edge, edgeType);
        }
    }

    private boolean isProjectClass(String className) {
        return className.startsWith(projectPackagePrefix) 
            && !className.matches("^(java|javax|sun|com.sun).*");
    }

    public Map<String, Object> toJson() {
        List<Map<String, String>> nodes = graph.vertexSet().stream()
                .map(v -> {
                    Map<String, String> node = new HashMap<>();
                    node.put("id", v);
                    node.put("type", nodeTypes.getOrDefault(v, "unknown"));
                    return node;
                })
                .collect(Collectors.toList());

        List<Map<String, String>> edges = graph.edgeSet().stream()
                .map(e -> {
                    Map<String, String> edge = new HashMap<>();
                    edge.put("source", graph.getEdgeSource(e));
                    edge.put("target", graph.getEdgeTarget(e));
                    edge.put("type", edgeTypes.getOrDefault(e, "unknown"));
                    return edge;
                })
                .collect(Collectors.toList());

        Map<String, Object> json = new HashMap<>();
        json.put("nodes", nodes);
        json.put("edges", edges);
        return json;
    }

    public Graph<String, DefaultEdge> getGraph() {
        return graph;
    }
    
    public Map<DefaultEdge, String> getEdgeTypes() {
        return edgeTypes;
    }

    public String getProjectPackage() {
        return projectPackagePrefix;
    }
}
