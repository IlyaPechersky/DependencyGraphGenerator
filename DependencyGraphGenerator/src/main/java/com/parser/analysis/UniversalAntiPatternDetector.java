package com.parser.analysis;

import com.parser.config.AntipatternConfig;
import com.parser.config.CheckConfig;
import com.parser.model.GraphData;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;
import java.lang.Number;

public class UniversalAntiPatternDetector {
    private final GraphData graphData;

    public UniversalAntiPatternDetector(GraphData graphData) {
        this.graphData = graphData;
    }

    public Map<String, Object> detect(AntipatternConfig config) {
        Map<String, Object> results = new HashMap<>();

        for (CheckConfig check : config.getChecks()) {
            switch (check.getType().toUpperCase()) {
                case "CYCLE":
                    results.put("cycles", detectCycles(check.getParams()));
                    break;
                case "GOD_CLASS":
                    results.put("godClasses", detectGodClasses(check.getParams()));
                    break;
                case "BOTTLENECK":
                    results.put("bottlenecks", detectBottlenecks(check.getParams()));
                    break;
                case "SERVICE_CHAIN":
                    results.put("serviceChains", detectServiceChains(check.getParams()));
                    break;
                case "HUB_LIKE_DEPENDENCY":
                    results.put("hubLikeDependencies", detectHubLikeDependencies(check.getParams()));
                    break;
                case "CHATTY_SERVICE":
                    results.put("chattyServices", detectChattyServices(check.getParams()));
                    break;
                default:
                    System.err.println("Unknown check type: " + check.getType());
            }
        }

        return results;
    }

    private List<List<String>> detectCycles(Map<String, Object> params) {
        CycleDetector<String, DefaultEdge> detector = new CycleDetector<>(graphData.getGraph());
        List<List<String>> cycles = new ArrayList<>();

        if (detector.detectCycles()) {
            for (String cycle : detector.findCycles()) {
                List<String> cyclePath = new ArrayList<>();
                cyclePath.add(cycle);
                cycles.add(cyclePath);
            }
        }
        return cycles;
    }

    private Map<String, Double> detectGodClasses(Map<String, Object> params) {
        PageRank<String, DefaultEdge> pr = new PageRank<>(graphData.getGraph());
        double threshold = (double) params.getOrDefault("threshold", 0.15);

        return graphData.getGraph().vertexSet().stream()
                .filter(v -> pr.getVertexScore(v) > threshold) // Фильтруем по threshold
                .collect(Collectors.toMap(
                        v -> v,
                        pr::getVertexScore
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()) // Сортируем по убыванию
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Double> detectBottlenecks(Map<String, Object> params) {
        BetweennessCentrality<String, DefaultEdge> bc = new BetweennessCentrality<>(graphData.getGraph());
        double threshold = (double) params.getOrDefault("threshold", 0.1);

        double maxCentrality = graphData.getGraph().vertexSet().stream()
                .mapToDouble(bc::getVertexScore)
                .max()
                .orElse(1.0);

        return graphData.getGraph().vertexSet().stream()
                .filter(v -> (bc.getVertexScore(v) / maxCentrality > threshold))
                .collect(Collectors.toMap(
                        v -> v,
                        v -> bc.getVertexScore(v) / maxCentrality,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                    )
                );
    }

    private Map<String, List<String>> detectServiceChains(Map<String, Object> params) {
        int maxChainLength = ((Number) params.getOrDefault("maxChainLength", 5)).intValue();

        Map<String, List<String>> chains = new HashMap<>();

        for (String vertex : graphData.getGraph().vertexSet()) {
            List<String> chain = new ArrayList<>();
            findChains(vertex, chain, chains, maxChainLength);
        }

        return chains;
    }

    private void findChains(String current, List<String> chain, Map<String, List<String>> chains, int maxLength) {
        chain.add(current);

        if (chain.size() > maxLength) {
            chains.put(current, new ArrayList<>(chain));
            return;
        }

        for (DefaultEdge edge : graphData.getGraph().outgoingEdgesOf(current)) {
            String target = graphData.getGraph().getEdgeTarget(edge);
            findChains(target, chain, chains, maxLength);
        }

        chain.remove(chain.size() - 1);
    }

    private Map<String, Integer> detectHubLikeDependencies(Map<String, Object> params) {
        int minDependencies = ((Number) params.getOrDefault("minDependencies", 10)).intValue();
        Map<String, Integer> hubs = new HashMap<>();

        for (String vertex : graphData.getGraph().vertexSet()) {
            int inDegree = graphData.getGraph().inDegreeOf(vertex);
            int outDegree = graphData.getGraph().outDegreeOf(vertex);

            if (inDegree + outDegree >= minDependencies) {
                hubs.put(vertex, inDegree + outDegree);
            }
        }

        return hubs;
    }

    private Map<String, Integer> detectChattyServices(Map<String, Object> params) {
        int minMessages = ((Number) params.getOrDefault("minMessages", 20)).intValue();
        Map<String, Integer> chattyServices = new HashMap<>();

        for (String vertex : graphData.getGraph().vertexSet()) {
            int outgoingEdges = graphData.getGraph().outDegreeOf(vertex);

            if (outgoingEdges >= minMessages) {
                chattyServices.put(vertex, outgoingEdges);
            }
        }

        return chattyServices;
    }
}
