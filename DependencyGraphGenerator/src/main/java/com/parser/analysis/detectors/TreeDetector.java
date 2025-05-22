package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;

public class TreeDetector extends BaseDetector implements TopologyDetector {

    @Override
    public Map<String, List<List<String>>> detect(GraphData graphData, Map<String, Object> params) {
        Graph<String, DefaultEdge> graph = graphData.getGraph();
        int minNodes = getIntParam(params, "min_nodes", 3);

        Map<String, List<List<String>>> trees = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();

        for (String root : graph.vertexSet()) {
            if (!visited.contains(root)) {
                List<List<String>> treeEdges = new ArrayList<>();
                Set<String> treeNodes = new HashSet<>();
                boolean isTree = traverseTree(root, null, graph, treeEdges, treeNodes, visited, graphData);

                if (isTree && treeNodes.size() >= minNodes) {
                    trees.put("Tree (Root: " + root + ")", treeEdges);
                    visited.addAll(treeNodes);
                }
            }
        }

        return trees;
    }

    private boolean traverseTree(String current,
                                String parent,
                                Graph<String, DefaultEdge> graph,
                                List<List<String>> treeEdges,
                                Set<String> treeNodes,
                                Set<String> globalVisited,
                                GraphData graphData) {
        if (treeNodes.contains(current)) return false;
        treeNodes.add(current);

        boolean isValidTree = true;
        for (DefaultEdge edge : graph.outgoingEdgesOf(current)) {
            String child = graph.getEdgeTarget(edge);
            if (child.equals(parent)) continue;

            if (!traverseTree(child, current, graph, treeEdges, treeNodes, globalVisited, graphData)) {
                isValidTree = false;
            } else {
                String type = graphData.getEdgeTypes().getOrDefault(edge, "unknown");
                treeEdges.add(Arrays.asList(current, child, type));
            }
        }

        globalVisited.add(current);
        return isValidTree;
    }
}
