package com.parser.analysis;

import com.parser.config.AntipatternConfig;
import com.parser.config.CheckConfig;
import com.parser.model.GraphData;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;

public class UniversalAntiPatternDetector {
    private final GraphData graphData;

    public UniversalAntiPatternDetector(GraphData graphData) {
        this.graphData = graphData;
    }

    public Map<String, Object> detect(AntipatternConfig config) {
        Map<String, Object> results = new LinkedHashMap<>();
        
        for (CheckConfig check : config.getChecks()) {
            TopologyDetector detector = DetectorFactory.getDetector(check.getTopology());
            if (detector == null) {
                System.err.println("Unknown topology: " + check.getTopology());
                continue;
            }
            
            Object detectionResult = detector.detect(
                graphData, 
                check.getParams()
            );
            
            String key = check.getType() + ", description: " + check.getDescription();
            results.put(key , detectionResult);
        }
        
        return results;
    }
}
