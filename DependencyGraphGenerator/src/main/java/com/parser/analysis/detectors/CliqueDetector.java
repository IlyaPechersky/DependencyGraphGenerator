package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import com.parser.model.GraphData;
import org.jgrapht.alg.clique.BronKerboschCliqueFinder;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;

public class CliqueDetector extends BaseDetector implements TopologyDetector {
    private static final int MAX_CLIQUE_SIZE = 6;
    
    @Override
    public Map<String, List<List<String>>> detect(GraphData graphData, Map<String, Object> params) {
        int minSize = getIntParam(params, "min_size", 3);
        int maxSize = getIntParam(params, "max_size", MAX_CLIQUE_SIZE);
        
        Graph<String, DefaultEdge> graph = graphData.getGraph();
        BronKerboschCliqueFinder<String, DefaultEdge> finder = new BronKerboschCliqueFinder<>(graph);
        
        Map<String, List<List<String>>> result = new LinkedHashMap<>();
        int cliqueNumber = 1;
        
        for (Set<String> clique : finder) {
            if (clique.size() >= minSize && clique.size() <= maxSize) {
                List<List<String>> edges = convertCliqueToEdges(clique, graph);
                result.put("Clique " + cliqueNumber++, edges);
                if (cliqueNumber > 100) break; // Лимит для безопасности
            }
        }
        
        return result;
    }
    
    private List<List<String>> convertCliqueToEdges(Set<String> clique, Graph<String, DefaultEdge> graph) {
        List<List<String>> edges = new ArrayList<>();
        List<String> nodes = new ArrayList<>(clique);
        
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i+1; j < nodes.size(); j++) {
                if (graph.containsEdge(nodes.get(i), nodes.get(j))) {
                    edges.add(Arrays.asList(nodes.get(i), nodes.get(j), "direct"));
                }
                if (graph.containsEdge(nodes.get(j), nodes.get(i))) {
                    edges.add(Arrays.asList(nodes.get(j), nodes.get(i), "direct"));
                }
            }
        }
        return edges;
    }
}
