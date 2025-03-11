package com.parser.visitors;

import com.parser.model.GraphData;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.Node;

import java.util.Optional;


public class ClassVisitor extends VoidVisitorAdapter<Void> {
    private final GraphData graphData;

    public ClassVisitor(GraphData graphData) {
        this.graphData = graphData;
    }

    private String resolveFullTypeName(Type type) {
        try {
            return type.resolve().describe();
        } catch (Exception e) {
            System.err.println("Failed to resolve type: " + type + ". Error: " + e.getMessage());
            return extractPackageFromContext(type) + type.asString();
        }
    }

    private String extractPackageFromContext(Type type) {
        Optional<Node> parentNode = type.getParentNode();
        
        if (parentNode.isPresent()) {
            Optional<ClassOrInterfaceDeclaration> container = parentNode.get().findAncestor(ClassOrInterfaceDeclaration.class);
            
            if (container.isPresent()) {
                String containerName = container.get().getFullyQualifiedName().orElse("");
                int lastDot = containerName.lastIndexOf('.');
                return lastDot > 0 ? containerName.substring(0, lastDot) + "." : "";
            }
        }
        return "unknown.package.";
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        if (n.isInterface()) return;

        String className = n.getFullyQualifiedName().orElse(n.getNameAsString());
        graphData.addNode(className, "class");

        // Наследование (extends)
        n.getExtendedTypes().forEach(type -> {
            String parent = resolveFullTypeName(type);
            graphData.addNode(parent, "class");
            graphData.addEdge(className, parent, "extends");
        });

        // Реализация интерфейсов (implements)
        n.getImplementedTypes().forEach(type -> {
            String interfaceName = resolveFullTypeName(type);
            graphData.addNode(interfaceName, "interface");
            graphData.addEdge(className, interfaceName, "implements");
        });

        // Fields
        n.getFields().forEach(field -> {
            field.getVariables().forEach(variable -> {
                String fieldType = resolveFullTypeName(variable.getType());
                graphData.addNode(fieldType, "unknown");
                graphData.addEdge(className, fieldType, "field");
            });
        });

        // Methods
        n.getMethods().forEach(method -> {
            // Return type
            if (method.getType() != null) {
                String returnType = resolveFullTypeName(method.getType());
                graphData.addNode(returnType, "unknown");
                graphData.addEdge(className, returnType, "method_return");
            }

            // Parameters
            method.getParameters().forEach(param -> {
                String paramType = resolveFullTypeName(param.getType());
                graphData.addNode(paramType, "unknown");
                graphData.addEdge(className, paramType, "method_parameter");
            });
        });

        // Annotations
        n.getAnnotations().forEach(annotation -> {
            String annotationType = annotation.getNameAsString();
            graphData.addNode(annotationType, "annotation");
            graphData.addEdge(className, annotationType, "class_annotation");
        });


        super.visit(n, arg);
    }
}
