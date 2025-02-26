package com.parser.visitors;

import com.parser.model.GraphData;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ClassVisitor extends VoidVisitorAdapter<Void> {
    private final GraphData graphData;

    public ClassVisitor(GraphData graphData) {
        this.graphData = graphData;
    }

    private String getRawType(String typeName) {
        return typeName
            .replaceAll("<.*>", "") // Удаляем generics
            .replaceAll("\\[\\]", "") // Удаляем массивы
            .replaceAll("@.*", "") // Удаляем аннотации
            .trim();
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        if (n.isInterface()) return;

        String className = n.getNameAsString();
        graphData.addNode(className, "class");

        // Наследование (extends)
        n.getExtendedTypes().forEach(type -> {
            String parent = type.getNameAsString();
            graphData.addNode(parent, "class");
            graphData.addEdge(className, parent, "extends");
        });

        // Реализация интерфейсов (implements)
        n.getImplementedTypes().forEach(type -> {
            String interfaceName = type.getNameAsString();
            graphData.addNode(interfaceName, "interface");
            graphData.addEdge(className, interfaceName, "implements");
        });

        // Fields
        n.getFields().forEach(field -> {
            field.getVariables().forEach(variable -> {
                String fieldType = getRawType(variable.getTypeAsString());
                graphData.addNode(fieldType, "unknown");
                graphData.addEdge(className, fieldType, "field");
            });
        });

        // Methods
        n.getMethods().forEach(method -> {
            // Return type
            if (method.getType() != null) {
                String returnType = getRawType(method.getType().toString());
                graphData.addNode(returnType, "unknown");
                graphData.addEdge(className, returnType, "method_return");
            }

            // Parameters
            method.getParameters().forEach(param -> {
                String paramType = getRawType(param.getType().asString());
                graphData.addNode(paramType, "unknown");
                graphData.addEdge(className, paramType, "method_parameter");
            });
        });

        // Annotations
        n.getAnnotations().forEach(annotation -> {
            String annotationType = getRawType(annotation.getNameAsString());
            graphData.addNode(annotationType, "annotation");
            graphData.addEdge(className, annotationType, "class_annotation");
        });


        super.visit(n, arg);
    }
}
