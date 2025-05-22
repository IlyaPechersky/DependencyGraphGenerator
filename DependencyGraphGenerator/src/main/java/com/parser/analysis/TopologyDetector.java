package com.parser.analysis;

import com.parser.model.GraphData;
import java.util.Map;
import java.util.List;

public interface TopologyDetector {
    Map<String, List<List<String>>> detect(GraphData graphData, Map<String, Object> params);
}