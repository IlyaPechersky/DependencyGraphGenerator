// CycleDetectorImpl.java
package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;

public class CycleDetectorImpl extends BaseDetector implements TopologyDetector {

    @Override
    public Map<String, List<List<String>>> detect(GraphData graphData, Map<String, Object> params) {
        int minLength = getIntParam(params, "min_length", 3);
        int maxLength = getIntParam(params, "max_length", 6);
        Graph<String, DefaultEdge> graph = graphData.getGraph();
        
        TarjanSimpleCycles<String, DefaultEdge> cycleFinder = new TarjanSimpleCycles<>(graph);
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        Set<Set<String>> uniqueCycles = new HashSet<>();

        List<List<String>> vertexCycles = cycleFinder.findSimpleCycles();
        int cycleNumber = 1;

        for (List<String> vertexCycle : vertexCycles) {
            // if (vertexCycle.size() < minLength || vertexCycle.size() > maxLength) continue;

            Set<String> cycleSet = new LinkedHashSet<>(vertexCycle);
            if (cycleSet.size() < 2 || uniqueCycles.contains(cycleSet)) continue;
            uniqueCycles.add(cycleSet);

            List<List<String>> edges = new ArrayList<>();
            Map<String, Set<String>> edgeTypesMap = new HashMap<>();

            for (int i = 0; i < vertexCycle.size(); i++) {
                String source = vertexCycle.get(i);
                String target = vertexCycle.get((i + 1) % vertexCycle.size());

                graph.getAllEdges(source, target).forEach(edge -> {
                    String edgeType = graphData.getEdgeTypes().getOrDefault(edge, "unknown");
                    edgeTypesMap.computeIfAbsent(source + "->" + target, k -> new HashSet<>())
                                 .add(edgeType);
                });
            }

            edgeTypesMap.forEach((k, v) -> {
                String[] nodes = k.split("->");
                v.forEach(type -> edges.add(Arrays.asList(nodes[0], nodes[1], type)));
            });

            if (!edges.isEmpty()) {
                result.put("Cycle " + cycleNumber++, edges);
            }
        }

        return result;
    }
}
