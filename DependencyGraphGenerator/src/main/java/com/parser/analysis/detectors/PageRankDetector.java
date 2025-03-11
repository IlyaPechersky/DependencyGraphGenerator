package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;
import java.util.stream.*;

public class PageRankDetector extends BaseDetector implements TopologyDetector {

    @Override
    public Map<String, Double> detect(Graph<String, DefaultEdge> graph, Map<String, Object> params) {
        double threshold = getDoubleParam(params, "threshold", 0.1);
        PageRank<String, DefaultEdge> pr = new PageRank<>(graph);

        return graph.vertexSet().stream()
            .filter(v -> pr.getVertexScore(v) > threshold)
            .collect(Collectors.toMap(
                    v -> v,
                    pr::getVertexScore
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
            ));
    }

}
