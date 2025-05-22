package com.parser.analysis;

import com.parser.model.GraphData;
import java.util.Map;
import java.util.List;

public interface MetricsDetector {
    Map<String, Double> detectMetrics(GraphData graphData, Map<String, Object> params);
}
