// StarDetector.java
package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;

public class StarDetector extends BaseDetector implements TopologyDetector {

    @Override
    public Map<String, List<List<String>>> detect(GraphData graphData, Map<String, Object> params) {
        Graph<String, DefaultEdge> graph = graphData.getGraph();
        int minRays = getIntParam(params, "min_rays", 3);
        boolean checkPurity = getBoolParam(params, "check_purity", true);
        String direction = getStringParam(params, "direction", "any");
        
        Map<String, List<List<String>>> stars = new LinkedHashMap<>();
        Set<String> processedCenters = new HashSet<>();

        for (String center : graph.vertexSet()) {
            // Собираем все связи центра
            Map<String, List<DefaultEdge>> rays = new HashMap<>();
            
            // Анализ исходящих и входящих ребер
            if (direction.equals("out") || direction.equals("any")) {
                graph.outgoingEdgesOf(center).forEach(e -> {
                    String target = graph.getEdgeTarget(e);
                    rays.computeIfAbsent(target, k -> new ArrayList<>()).add(e);
                });
            }
            
            if (direction.equals("in") || direction.equals("any")) {
                graph.incomingEdgesOf(center).forEach(e -> {
                    String source = graph.getEdgeSource(e);
                    rays.computeIfAbsent(source, k -> new ArrayList<>()).add(e);
                });
            }

            // Фильтрация по минимальному количеству лучей
            for (Map.Entry<String, List<DefaultEdge>> entry : rays.entrySet()) {
                String peripheral = entry.getKey();
                List<DefaultEdge> edges = entry.getValue();
                
                if (edges.size() >= minRays) {
                    // Проверка на чистоту звезды
                    if (checkPurity && !isPureStar(graph, center, peripheral, edges)) continue;
                    
                    // Генерация уникального ключа
                    String starKey = generateStarKey(center, edges, graphData);
                    if (!processedCenters.contains(starKey)) {
                        processedCenters.add(starKey);
                        addStarStructure(stars, center, peripheral, edges, graphData);
                    }
                }
            }
        }
        
        return stars;
    }

    private boolean isPureStar(Graph<String, DefaultEdge> graph, 
                              String center, 
                              String peripheral,
                              List<DefaultEdge> edges) {
        // Проверка, что периферийные узлы не связаны между собой
        Set<String> peripherals = new HashSet<>();
        edges.forEach(e -> {
            if (graph.getEdgeSource(e).equals(center)) {
                peripherals.add(graph.getEdgeTarget(e));
            } else {
                peripherals.add(graph.getEdgeSource(e));
            }
        });
        
        for (String p1 : peripherals) {
            for (String p2 : peripherals) {
                if (!p1.equals(p2) && graph.containsEdge(p1, p2)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String generateStarKey(String center, 
                                  List<DefaultEdge> edges, 
                                  GraphData graphData) {
        // Уникальный ключ на основе типов ребер и направления
        List<String> edgeTypes = new ArrayList<>();
        for (DefaultEdge e : edges) {
            String type = graphData.getEdgeTypes().getOrDefault(e, "unknown");
            String direction = graphData.getGraph().getEdgeSource(e).equals(center) 
                ? "out" : "in";
            edgeTypes.add(direction + ":" + type);
        }
        Collections.sort(edgeTypes);
        return center + "|" + String.join(",", edgeTypes);
    }

    private void addStarStructure(Map<String, List<List<String>>> stars,
                                  String center,
                                  String peripheral,
                                  List<DefaultEdge> edges,
                                  GraphData graphData) {
        List<List<String>> starEdges = new ArrayList<>();
        
        for (DefaultEdge edge : edges) {
            String source = graphData.getGraph().getEdgeSource(edge);
            String target = graphData.getGraph().getEdgeTarget(edge);
            String type = graphData.getEdgeTypes().getOrDefault(edge, "unknown");
            
            starEdges.add(Arrays.asList(source, target, type));
        }
        
        stars.put("Star [" + center + "]", starEdges);
    }
}
