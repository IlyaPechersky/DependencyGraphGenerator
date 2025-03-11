package com.parser.analysis;

import com.parser.analysis.detectors.*;
import java.util.*;


public class DetectorFactory {
    private static final Map<String, TopologyDetector> DETECTORS = Map.of(
        "cycle", new CycleDetectorImpl(),
        "pagerank", new PageRankDetector(),
        "betweenness", new BetweennessDetector(),
        "bamboo", new BambooDetector(),
        "degree", new HubDetector(),
        "out_degree", new OutgoingEdgesDetector()
    );

    public static TopologyDetector getDetector(String topology) {
        return DETECTORS.getOrDefault(topology, null);
    }
}
