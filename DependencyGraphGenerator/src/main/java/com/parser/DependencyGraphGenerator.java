package com.parser;

import com.parser.analysis.TopologyDetector;
import com.parser.analysis.ConfigurableDetector;
import com.parser.config.AntiPatternConfigLoader;
import com.parser.config.AntiPatternConfig;
import com.parser.model.GraphData;
import com.parser.visitors.*;
import com.github.javaparser.*;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import com.github.javaparser.ast.*;
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
    private final CombinedTypeSolver typeSolver;
    private final JavaSymbolSolver symbolSolver;
    private final JavaParser javaParser;

    public DependencyGraphGenerator(String projectPackage, String sourceRoot) {
        this.graphData = new GraphData(projectPackage);
        
        // Единый TypeSolver для всего проекта
        this.typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(new File(sourceRoot)));

        addDependencies();

        this.symbolSolver = new JavaSymbolSolver(typeSolver);
        
        // Создаем конфигурацию парсера
        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(symbolSolver);
        this.javaParser = new JavaParser(config);
    }

    private void addDependencies() {
        String m2Repo = System.getProperty("user.home") + "/.m2/repository/";
        
        String[] deps = {
            "com/github/javaparser/javaparser-core/3.25.9/javaparser-core-3.25.9.jar",
            "com/github/javaparser/javaparser-symbol-solver-core/3.25.9/javaparser-symbol-solver-core-3.25.9.jar",
            "org/jgrapht/jgrapht-core/1.5.2/jgrapht-core-1.5.2.jar",
            "com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar"
        };

        for (String dep : deps) {
            try {
                typeSolver.add(new JarTypeSolver(m2Repo + dep));
                System.out.println("Added dependency: " + dep);
            } catch (IOException e) {
                System.err.println("Failed to load: " + dep);
            }
        }
    }

    public void parseDirectory(String directoryPath) throws IOException {
        List<Path> javaFiles = Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".java"))
                .toList();
        
        System.out.println("Found " + javaFiles.size() + " Java files to parse");
        
        for (Path file : javaFiles) {
            parseFile(file.toFile());
        }
    }

    private void parseFile(File file) {
        try {
            // Используем не-статический парсер
            ParseResult<CompilationUnit> parseResult = javaParser.parse(file);

            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();

                new AnnotationVisitor(graphData, symbolSolver).visit(cu, null);
                new ClassVisitor(graphData, symbolSolver).visit(cu, null);
                new EnumVisitor(graphData, symbolSolver).visit(cu, null);
                new InterfaceVisitor(graphData, symbolSolver).visit(cu, null);

                System.out.println("Processed: " + file.getName());
            } else {
                System.err.println("Failed to parse: " + file.getPath());
                parseResult.getProblems().forEach(problem -> 
                    System.err.println("  Problem: " + problem.getMessage()));
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getPath());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error in file: " + file.getPath());
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
        String projectPackage = "org.xwiki";
        String sourceRoot = "/Users/i-pechersky/VSCProjects/Parser/examples/xwiki-platform";
        DependencyGraphGenerator generator = new DependencyGraphGenerator(projectPackage, sourceRoot);
        try {
            generator.parseDirectory(sourceRoot);
            String report = generator.outputFullAnalysisAsJson("src/main/resources/antipatterns/custom.json");
            Files.write(Paths.get("architecture-analysis.json"), report.getBytes(StandardCharsets.UTF_8));
            System.out.println("Report generated: architecture-analysis.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
