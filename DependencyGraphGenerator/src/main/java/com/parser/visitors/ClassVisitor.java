package com.parser.visitors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;
import com.parser.model.GraphData;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;

public class ClassVisitor extends BaseVisitor {
    public ClassVisitor(GraphData graphData, JavaSymbolSolver symbolSolver) {
        super(graphData, symbolSolver);
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
        for (FieldDeclaration field : n.getFields()) {
            Type fieldType = field.getElementType();
            String resolvedType = resolveFullTypeName(fieldType);
            graphData.addNode(resolvedType, "type");
            graphData.addEdge(className, resolvedType, "field");
        }

        // Methods
        for (MethodDeclaration method : n.getMethods()) {
            // Return type
            if (method.getType() != null) {
                String returnType = resolveFullTypeName(method.getType());
                graphData.addNode(returnType, "type");
                graphData.addEdge(className, returnType, "method_return");
            }

            // Parameters
            method.getParameters().forEach(param -> {
                String paramType = resolveFullTypeName(param.getType());
                graphData.addNode(paramType, "type");
                graphData.addEdge(className, paramType, "method_parameter");
            });
        }

        // Annotations
        n.getAnnotations().forEach(annotation -> {
            String annotationType = resolveAnnotationName(annotation);
            graphData.addNode(annotationType, "annotation");
            graphData.addEdge(className, annotationType, "class_annotation");
        });

        super.visit(n, arg);
    }
}
