package com.parser.visitors;

import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.type.Type;
import com.parser.model.GraphData;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;

public class EnumVisitor extends BaseVisitor {
    public EnumVisitor(GraphData graphData, JavaSymbolSolver symbolSolver) {
        super(graphData, symbolSolver);
    }

    @Override
    public void visit(EnumDeclaration n, Void arg) {
        String enumName = n.getFullyQualifiedName().orElse(n.getNameAsString());
        graphData.addNode(enumName, "enum");

        // Реализация интерфейсов (implements)
        n.getImplementedTypes().forEach(type -> {
            String interfaceName = resolveFullTypeName(type);
            graphData.addNode(interfaceName, "interface");
            graphData.addEdge(enumName, interfaceName, "implements");
        });

        super.visit(n, arg);
    }
}
