package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;
import java.util.stream.Collectors;

public class StarDetector extends BaseDetector implements TopologyDetector {

    @Override
    public Map<String, List<List<String>>> detect(GraphData graphData, Map<String, Object> params) {
        List<String> allowedEdgeTypes = getListStringParam(params, "allowed_edge_types");
        Graph<String, DefaultEdge> graphToAnalyze;

        if (allowedEdgeTypes != null && !allowedEdgeTypes.isEmpty()) {
            graphToAnalyze = buildFilteredGraph(graphData, allowedEdgeTypes);
        } else {
            graphToAnalyze = graphData.getGraph();
        }

        int minRays = getIntParam(params, "min_rays", 3);
        String directionParam = getStringParam(params, "direction", "any").toLowerCase();
        
        Map<String, List<List<String>>> detectedStars = new LinkedHashMap<>();

        for (String potentialCenter : graphToAnalyze.vertexSet()) {
            List<DefaultEdge> incidentEdges = new ArrayList<>();
            if (directionParam.equals("out") || directionParam.equals("any")) {
                incidentEdges.addAll(graphToAnalyze.outgoingEdgesOf(potentialCenter));
            }
            if (directionParam.equals("in") || directionParam.equals("any")) {
                if (directionParam.equals("in") || (directionParam.equals("any") && graphToAnalyze.getType().isDirected())) {
                    incidentEdges.addAll(graphToAnalyze.incomingEdgesOf(potentialCenter));
                }
            }
            List<DefaultEdge> distinctIncidentEdges = incidentEdges.stream().distinct().collect(Collectors.toList());

            if (distinctIncidentEdges.size() >= minRays) {
                List<List<String>> starEdgesReport = new ArrayList<>();
                Set<String> peripherals = new HashSet<>();

                for (DefaultEdge edge : distinctIncidentEdges) {
                    String source = graphToAnalyze.getEdgeSource(edge);
                    String target = graphToAnalyze.getEdgeTarget(edge);
                    String originalEdgeType = graphData.getEdgeTypes().getOrDefault(edge, "unknown");
                    
                    starEdgesReport.add(Arrays.asList(source, target, originalEdgeType));
                    if (source.equals(potentialCenter)) peripherals.add(target);
                    else peripherals.add(source);
                }

                if (!starEdgesReport.isEmpty()) {
                    detectedStars.put("Star-" + potentialCenter, starEdgesReport);
                }
            }
        }
        return detectedStars;
    }

    private boolean isPureStarConfiguration(Graph<String, DefaultEdge> graph, String center, List<DefaultEdge> rays) {
        Set<String> peripheralNodes = new HashSet<>();
        for (DefaultEdge ray : rays) {
            String source = graph.getEdgeSource(ray);
            String target = graph.getEdgeTarget(ray);
            if (source.equals(center)) {
                peripheralNodes.add(target);
            } else if (target.equals(center)) {
                peripheralNodes.add(source);
            } else {
                return false; 
            }
        }

        if (peripheralNodes.size() < 2) return true;

        for (String p1 : peripheralNodes) {
            for (String p2 : peripheralNodes) {
                if (!p1.equals(p2)) {
                    if (graph.containsEdge(p1, p2) || graph.containsEdge(p2, p1)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
