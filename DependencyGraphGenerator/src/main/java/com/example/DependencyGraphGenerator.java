package com.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DependencyGraphGenerator {

    private Graph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
    private Map<String, Set<String>> dependencies = new HashMap<>();

    public void parseDirectory(String directoryPath) throws IOException {
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".java"))
                .forEach(file -> parseFile(file.toFile()));
    }

    private void parseFile(File file) {
        try {
            // Create an instance of JavaParser
            CompilationUnit cu = new JavaParser().parse(file).getResult().orElse(null);
            if (cu != null) {
                cu.accept(new ClassVisitor(), null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClassVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            String className = n.getNameAsString();
            graph.addVertex(className);
            dependencies.putIfAbsent(className, new HashSet<>());

            // Find dependencies (extends, implements)
            n.getExtendedTypes().forEach(type -> {
                String dependency = type.getNameAsString();
                graph.addVertex(dependency);
                graph.addEdge(className, dependency);
                dependencies.get(className).add(dependency);
            });

            super.visit(n, arg);
        }
    }

    public String outputGraphAsJson() {
        Map<String, Object> jsonOutput = new HashMap<>();
        jsonOutput.put("nodes", graph.vertexSet());
        jsonOutput.put("edges", graph.edgeSet());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(jsonOutput);
    }

    public static void main(String[] args) {
        DependencyGraphGenerator generator = new DependencyGraphGenerator();
        try {
            generator.parseDirectory("/Users/i-pechersky/VSCProjects/Parser/examples/PetClinic/spring-petclinic-microservices"); // Change this to your directory
            String jsonOutput = generator.outputGraphAsJson();
            System.out.println(jsonOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}