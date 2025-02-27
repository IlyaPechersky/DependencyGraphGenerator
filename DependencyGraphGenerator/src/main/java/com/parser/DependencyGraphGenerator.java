package com.parser;

import com.parser.analysis.UniversalAntiPatternDetector;
import com.parser.config.AntipatternConfig;
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
    private final UniversalAntiPatternDetector detector;

    public DependencyGraphGenerator(String projectPackage) {
        this.graphData = new GraphData(projectPackage);
        this.detector = new UniversalAntiPatternDetector(graphData);
    }

    public void parseDirectory(String directoryPath) throws IOException {
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".java"))
                .forEach(file -> parseFile(file.toFile()));
    }

    private void parseFile(File file) {
        try {
            // Настройка SymbolResolver
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

    public AntipatternConfig loadConfig(String configPath) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(configPath)));
        return new Gson().fromJson(json, AntipatternConfig.class);
    }

    public String outputFullAnalysisAsJson() {
        Map<String, Object> fullReport = new HashMap<>();
        fullReport.put("dependencyGraph", graphData.toJson());

        try {
            AntipatternConfig config = loadConfig("src/main/resources/antipatterns/custom.json");
            fullReport.put("architectureAnalysis", detector.detect(config));
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        String projectPackage = "org.springframework.samples.petclinic"; // depends on rep
        DependencyGraphGenerator generator = new DependencyGraphGenerator(projectPackage);
        try {
            generator.parseDirectory("/Users/i-pechersky/VSCProjects/Parser/examples/PetClinic/spring-petclinic-microservices"); // Change this to your directory
            String fullReport = generator.outputFullAnalysisAsJson();
            Files.write(Paths.get("architecture-analysis.json"), fullReport.getBytes(StandardCharsets.UTF_8));

            System.out.println("Анализ архитектуры сохранен в architecture-analysis.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
