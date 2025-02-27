package com.parser.visitors;

import com.parser.model.GraphData;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class InterfaceVisitor extends VoidVisitorAdapter<Void> {
    private final GraphData graphData;

    public InterfaceVisitor(GraphData graphData) {
        this.graphData = graphData;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        if (!n.isInterface()) return;

        String interfaceName = n.getFullyQualifiedName().orElse(n.getNameAsString());
        graphData.addNode(interfaceName, "interface");

        // Наследование интерфейсов (extends)
        n.getExtendedTypes().forEach(type -> {
            String parentInterface = type.getNameAsString();
            graphData.addNode(parentInterface, "interface");
            graphData.addEdge(interfaceName, parentInterface, "extends");
        });

        super.visit(n, arg);
    }
}
