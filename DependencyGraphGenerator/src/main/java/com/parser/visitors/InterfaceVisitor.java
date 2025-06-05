package com.parser.visitors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.Type;
import com.parser.model.GraphData;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;

public class InterfaceVisitor extends BaseVisitor {
    public InterfaceVisitor(GraphData graphData, JavaSymbolSolver symbolSolver) {
        super(graphData, symbolSolver);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        if (!n.isInterface()) return;

        String interfaceName = n.getFullyQualifiedName().orElse(n.getNameAsString());
        graphData.addNode(interfaceName, "interface");

        // Наследование интерфейсов (extends)
        n.getExtendedTypes().forEach(type -> {
            String parentInterface = resolveFullTypeName(type);
            graphData.addNode(parentInterface, "interface");
            graphData.addEdge(interfaceName, parentInterface, "extends");
        });

        // Default methods
        n.getMethods().forEach(method -> {
            if (method.getType() != null) {
                String returnType = resolveFullTypeName(method.getType());
                graphData.addNode(returnType, "type");
                graphData.addEdge(interfaceName, returnType, "method_return");
            }
        });

        // Annotations
        n.getAnnotations().forEach(annotation -> {
            String annotationType = resolveAnnotationName(annotation);
            graphData.addNode(annotationType, "annotation");
            graphData.addEdge(interfaceName, annotationType, "interface_annotation");
        });

        super.visit(n, arg);
    }
}
