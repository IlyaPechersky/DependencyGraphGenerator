package com.parser.analysis;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.Map;

public interface TopologyDetector {
    Object detect(Graph<String, DefaultEdge> graph, Map<String, Object> params);
}
