package com.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;


public class DependencyGraphGenerator {
    private Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    private Map<String, String> nodeTypes = new HashMap<>();
    private Map<DefaultEdge, String> edgeTypes = new HashMap<>();

    public void parseDirectory(String directoryPath) throws IOException {
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".java"))
                .forEach(file -> parseFile(file.toFile()));
    }

    private void parseFile(File file) {
        try {
            CompilationUnit cu = new JavaParser().parse(file).getResult().orElse(null);
            if (cu != null) {
                new ClassVisitor().visit(cu, null);
                new EnumVisitor().visit(cu, null);
                new AnnotationVisitor().visit(cu, null);
                new InterfaceVisitor().visit(cu, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getRawType(String typeName) {
        return typeName
            .replaceAll("<.*>", "") // Удаляем generics
            .replaceAll("\\[\\]", "") // Удаляем массивы
            .replaceAll("@.*", "") // Удаляем аннотации
            .trim();
    }

    private void addNode(String name, String type) {
        // Пропускаем примитивные типы и void
        if (name == null || name.isEmpty() || 
            name.matches("^(boolean|byte|char|short|int|long|float|double|void)$")) {
            return;
        }
        
        if (!graph.containsVertex(name)) {
            graph.addVertex(name);
            nodeTypes.put(name, type);
        }
    }

    private void addEdge(String source, String target, String edgeType) {
        if (source.equals(target) || 
            source.contains(".") || // Пропускаем FQN
            target.contains(".") ||
            !isProjectClass(target)) {
            return;
        }

        // Пропускаем петли и зависимости на примитивные типы
        if (source.equals(target) || !graph.containsVertex(source) || !graph.containsVertex(target)) {
            return;
        }
        
        DefaultEdge edge = graph.addEdge(source, target);
        if (edge != null) {
            edgeTypes.put(edge, edgeType);
        }
    }

    private boolean isProjectClass(String className) {
        return !className.matches("^(java|javax|sun|com.sun).*");
    }

    private class ClassVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            if (n.isInterface()) return;

            String className = n.getNameAsString();
            addNode(className, "class");

            // Inheritance
            n.getExtendedTypes().forEach(type -> {
                String parent = getRawType(type.getNameAsString());
                addNode(parent, "class");
                addEdge(className, parent, "extends");
            });

            // Implementations
            n.getImplementedTypes().forEach(type -> {
                String interfaceName = getRawType(type.getNameAsString());
                addNode(interfaceName, "interface");
                addEdge(className, interfaceName, "implements");
            });

            // Fields
            n.getFields().forEach(field -> {
                field.getVariables().forEach(variable -> {
                    String fieldType = getRawType(variable.getTypeAsString());
                    addNode(fieldType, "unknown");
                    addEdge(className, fieldType, "field");
                });
            });

            // Methods
            n.getMethods().forEach(method -> {
                // Return type
                if (method.getType() != null) {
                    String returnType = getRawType(method.getType().toString());
                    addNode(returnType, "unknown");
                    addEdge(className, returnType, "method_return");
                }

                // Parameters
                method.getParameters().forEach(param -> {
                    String paramType = getRawType(param.getType().asString());
                    addNode(paramType, "unknown");
                    addEdge(className, paramType, "method_parameter");
                });
            });

            // Annotations
            n.getAnnotations().forEach(annotation -> {
                String annotationType = getRawType(annotation.getNameAsString());
                addNode(annotationType, "annotation");
                addEdge(className, annotationType, "class_annotation");
            });

            super.visit(n, arg);
        }
    }

    private class InterfaceVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            if (!n.isInterface()) return;

            String interfaceName = n.getNameAsString();
            addNode(interfaceName, "interface");

            // Interface extends
            n.getExtendedTypes().forEach(type -> {
                String parentInterface = getRawType(type.getNameAsString());
                addNode(parentInterface, "interface");
                addEdge(interfaceName, parentInterface, "extends");
            });
        }
    }

    private class EnumVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(EnumDeclaration n, Void arg) {
            String enumName = n.getNameAsString();
            addNode(enumName, "enum");

            // Implemented interfaces
            n.getImplementedTypes().forEach(type -> {
                String interfaceName = getRawType(type.getNameAsString());
                addNode(interfaceName, "interface");
                addEdge(enumName, interfaceName, "implements");
            });
        }
    }

    private class AnnotationVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(AnnotationDeclaration n, Void arg) {
            String annotationName = n.getNameAsString();
            addNode(annotationName, "annotation");
        }
    }

    public Map<String, Object> analyzeArchitecture() {
        Map<String, Object> analysis = new HashMap<>();

        analysis.put("cycles", detectCycles());
        analysis.put("godClasses", detectGodClasses());
        analysis.put("bottlenecks", detectBottlenecks());

        return analysis;
    }

    private List<List<String>> detectCycles() {
        CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(graph);
        List<List<String>> cycles = new ArrayList<>();
        
        if (cycleDetector.detectCycles()) {
            for (String cycle : cycleDetector.findCycles()) {
                List<String> cyclePath = new ArrayList<>();
                cyclePath.add(cycle);
                cycles.add(cyclePath);
            }
        }
        return cycles;
    }

    private Map<String, Double> detectGodClasses() {
        PageRank<String, DefaultEdge> pr = new PageRank<>(graph);
        
        return graph.vertexSet().stream()
            .collect(Collectors.toMap(
                v -> v,
                pr::getVertexScore
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    private Map<String, Double> detectBottlenecks() {
        BetweennessCentrality<String, DefaultEdge> bc = new BetweennessCentrality<>(graph);
        
        return graph.vertexSet().stream()
            .collect(Collectors.toMap(
                v -> v,
                bc::getVertexScore
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    public Map<String, Object> outputGraphAsJson() {
        List<Map<String, String>> nodes = graph.vertexSet().stream()
                .map(v -> {
                    Map<String, String> node = new HashMap<>();
                    node.put("id", v);
                    node.put("type", nodeTypes.getOrDefault(v, "unknown"));
                    return node;
                })
                .collect(Collectors.toList());

        List<Map<String, String>> edges = graph.edgeSet().stream()
                .map(e -> {
                    Map<String, String> edge = new HashMap<>();
                    edge.put("source", graph.getEdgeSource(e));
                    edge.put("target", graph.getEdgeTarget(e));
                    edge.put("type", edgeTypes.getOrDefault(e, "unknown"));
                    return edge;
                })
                .collect(Collectors.toList());

        Map<String, Object> json = new HashMap<>();
        json.put("nodes", nodes);
        json.put("edges", edges);
        return json;
    }

    public String outputFullAnalysisAsJson() {
        Map<String, Object> fullReport = new HashMap<>();
        
        fullReport.put("dependencyGraph", outputGraphAsJson());
        fullReport.put("architectureAnalysis", analyzeArchitecture());

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
        DependencyGraphGenerator generator = new DependencyGraphGenerator();
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