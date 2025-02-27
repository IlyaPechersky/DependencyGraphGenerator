package com.parser.visitors;

import com.parser.model.GraphData;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class EnumVisitor extends VoidVisitorAdapter<Void> {
    private final GraphData graphData;

    public EnumVisitor(GraphData graphData) {
        this.graphData = graphData;
    }

    @Override
    public void visit(EnumDeclaration n, Void arg) {
        String enumName = n.getFullyQualifiedName().orElse(n.getNameAsString());
        graphData.addNode(enumName, "enum");

        // Реализация интерфейсов (implements)
        n.getImplementedTypes().forEach(type -> {
            String interfaceName = type.getNameAsString();
            graphData.addNode(interfaceName, "interface");
            graphData.addEdge(enumName, interfaceName, "implements");
        });

        super.visit(n, arg);
    }
}
