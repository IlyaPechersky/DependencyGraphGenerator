package com.parser;

import com.parser.analysis.TopologyDetector;
import com.parser.analysis.ConfigurableDetector;
import com.parser.config.AntiPatternConfigLoader;
import com.parser.config.AntiPatternConfig;
import com.parser.model.GraphData;
import com.parser.visitors.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import com.github.javaparser.ast.CompilationUnit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class DependencyGraphGenerator {
    private final GraphData graphData;

    public DependencyGraphGenerator(String projectPackage) {
        this.graphData = new GraphData(projectPackage);
    }

    public void parseDirectory(String directoryPath) throws IOException {
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".java"))
                .forEach(file -> parseFile(file.toFile()));
    }

    private void parseFile(File file) {
        try {
            CombinedTypeSolver typeSolver = new CombinedTypeSolver();
            typeSolver.add(new JavaParserTypeSolver(file.getParentFile()));
            typeSolver.add(new ReflectionTypeSolver());

            ParserConfiguration config = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

            JavaParser parser = new JavaParser(config);
            
            ParseResult<CompilationUnit> parseResult = parser.parse(file);
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                new ClassVisitor(graphData).visit(cu, null);
                new EnumVisitor(graphData).visit(cu, null);
                new AnnotationVisitor(graphData).visit(cu, null);
                new InterfaceVisitor(graphData).visit(cu, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> detectAntiPatterns(String configPath) {
        Map<String, Object> report = new HashMap<>();
        try {
            AntiPatternConfig config = AntiPatternConfigLoader.load(configPath);
            
            config.getAntiPatterns().forEach((patternName, definition) -> {
                ConfigurableDetector detector = new ConfigurableDetector(definition);
                
                Map<String, List<List<String>>> topologicalFindings = detector.detect(graphData, null);
                Map<String, Double> metricFindings = detector.detectMetrics(graphData, null);
                
                boolean hasTopological = topologicalFindings != null && !topologicalFindings.isEmpty();
                boolean hasMetric = metricFindings != null && !metricFindings.isEmpty();

                if (hasTopological && !hasMetric) {
                    report.put(patternName, topologicalFindings);
                } else if (!hasTopological && hasMetric) {
                    report.put(patternName, metricFindings);
                } else if (hasTopological && hasMetric) {
                    Map<String, Object> combinedFindings = new HashMap<>();
                    combinedFindings.put("topological", topologicalFindings);
                    combinedFindings.put("metric", metricFindings);
                    report.put(patternName, combinedFindings);
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return report;
    }

    public String outputFullAnalysisAsJson(String configPath) {
        Map<String, Object> fullReport = new HashMap<>();
        fullReport.put("dependencyGraph", graphData.toJson());
        fullReport.put("architectureAnalysis", detectAntiPatterns(configPath));

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, type, context) -> {
                    if (src == src.longValue()) {
                        return new JsonPrimitive(src.longValue());
                    }
                    return new JsonPrimitive(Math.round(src * 100.0) / 100.0);
                })
                .create();

        return gson.toJson(fullReport);
    }

    public static void main(String[] args) {
        String projectPackage = "org.springframework.samples.petclinic";
        DependencyGraphGenerator generator = new DependencyGraphGenerator(projectPackage);
        try {
            generator.parseDirectory("/Users/i-pechersky/VSCProjects/Parser/examples/PetClinic/spring-petclinic-microservices"); // Change this to your directory
            String report = generator.outputFullAnalysisAsJson("src/main/resources/antipatterns/custom.json");
            Files.write(Paths.get("architecture-analysis.json"), report.getBytes());
            System.out.println("Report generated: architecture-analysis.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
