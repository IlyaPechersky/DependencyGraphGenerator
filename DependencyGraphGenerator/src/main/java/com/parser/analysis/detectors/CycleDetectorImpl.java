package com.parser.analysis.detectors;

import com.parser.analysis.TopologyDetector;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;


public class CycleDetectorImpl extends BaseDetector implements TopologyDetector {
    @Override
    public List<String> detect(Graph<String, DefaultEdge> graph, Map<String, Object> params) {
        CycleDetector<String, DefaultEdge> detector = new CycleDetector<>(graph);
        List<String> cycles = new ArrayList<>();
        
        if (detector.detectCycles()) {
            detector.findCycles().forEach(cycle -> 
                cycles.add(cycle));
        }
        return cycles;
    }
}
