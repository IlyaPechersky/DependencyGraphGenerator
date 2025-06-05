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
        List<String> allowedEdgeTypes = getListStringParam(params, "allowed_edge_types");
        Graph<String, DefaultEdge> graphToAnalyze;

        if (allowedEdgeTypes != null && !allowedEdgeTypes.isEmpty()) {
            graphToAnalyze = buildFilteredGraph(graphData, allowedEdgeTypes);
        } else {
            graphToAnalyze = graphData.getGraph();
        }

        // int minLength = getIntParam(params, "min_length", 3); // Retain if needed for cycle properties
        // int maxLength = getIntParam(params, "max_length", 6); // Retain if needed
        
        // Ensure the graph is directed, as TarjanSimpleCycles expects a directed graph.
        // buildFilteredGraph should preserve directedness from graphData.getGraph().
        if (!graphToAnalyze.getType().isDirected()) {
            // This should not happen if graphData.getGraph() is always directed.
            System.err.println("Warning: CycleDetectorImpl received an undirected graph. TarjanSimpleCycles might not work as expected.");
            // Optionally, convert to directed or return empty if cycles are not meaningful on undirected for this detector.
            return Collections.emptyMap(); 
        }

        TarjanSimpleCycles<String, DefaultEdge> cycleFinder = new TarjanSimpleCycles<>(graphToAnalyze);
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        Set<Set<String>> uniqueCyclesNodes = new HashSet<>(); // To store unique sets of nodes in a cycle

        List<List<String>> vertexCycles = cycleFinder.findSimpleCycles();
        int cycleNumber = 1;

        for (List<String> vertexCycle : vertexCycles) {
            // Example: if (vertexCycle.size() < minLength || vertexCycle.size() > maxLength) continue;

            Set<String> cycleNodeSet = new LinkedHashSet<>(vertexCycle);
            // A cycle needs at least 2 distinct nodes to form an edge back, though Tarjan usually gives >=3 for simple cycles.
            // Or adjust based on minLength parameter if re-enabled.
            if (cycleNodeSet.size() < 2 || uniqueCyclesNodes.contains(cycleNodeSet)) continue; 
            uniqueCyclesNodes.add(cycleNodeSet);

            List<List<String>> cycleEdgesWithTypes = new ArrayList<>();

            for (int i = 0; i < vertexCycle.size(); i++) {
                String source = vertexCycle.get(i);
                String target = vertexCycle.get((i + 1) % vertexCycle.size());

                // In the graphToAnalyze (which might be filtered), get the edges.
                // The edge types are still referenced from the original graphData for reporting.
                Set<DefaultEdge> edgesBetweenNodes = graphToAnalyze.getAllEdges(source, target);
                if (edgesBetweenNodes != null) {
                    for (DefaultEdge edge : edgesBetweenNodes) {
                        String edgeType = graphData.getEdgeTypes().getOrDefault(edge, "unknown"); // Get type from original GraphData
                        cycleEdgesWithTypes.add(Arrays.asList(source, target, edgeType));
                    }
                }
            }

            if (!cycleEdgesWithTypes.isEmpty()) {
                result.put("Cycle " + cycleNumber++, cycleEdgesWithTypes);
            }
        }
        return result;
    }
}
