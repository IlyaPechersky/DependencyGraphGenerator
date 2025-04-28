package com.parser.analysis;

import com.parser.model.GraphData;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import java.util.Map;

public interface TopologyDetector {
    Object detect(GraphData graphData, Map<String, Object> params);
}
