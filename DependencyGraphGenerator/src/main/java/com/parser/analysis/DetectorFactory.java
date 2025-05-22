package com.parser.analysis;

import com.parser.analysis.detectors.*;
import com.parser.config.AntiPatternConfig;
import com.parser.config.AntiPatternConfigLoader;
import java.util.*;
import java.util.stream.Collectors;

public class DetectorFactory {
    private static final Map<String, TopologyDetector> TOPOLOGY_DETECTORS = Map.of(
        "cycle", new CycleDetectorImpl(),
        "bamboo", new BambooDetector(),
        "star", new StarDetector(),
        "clique", new CliqueDetector(),
        "tree", new TreeDetector()
    );

    private static final Map<String, MetricsDetector> METRIC_DETECTORS = Map.of(
        "connectivity", new ConnectivityDetector(),
        "betweenness", new BetweennessDetector(),
        "pagerank", new PageRankDetector()
    );

    public static TopologyDetector getTopologyDetector(String topology) {
        if (topology == null) {
            throw new IllegalArgumentException("Detector name cannot be null");
        }
        
        TopologyDetector detector = TOPOLOGY_DETECTORS.get(topology.toLowerCase());
        if (detector == null) {
            System.err.println("Detector not found: " + topology);
        }
        return detector;
    }

    public static MetricsDetector getMetricDetector(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Detector name cannot be null");
        }
        return METRIC_DETECTORS.get(name.toLowerCase());
    }
}
