package com.parser.visitors;

import com.parser.model.GraphData;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;

public class AnnotationVisitor extends BaseVisitor {
    public AnnotationVisitor(GraphData graphData, JavaSymbolSolver symbolSolver) {
        super(graphData, symbolSolver);
    }

    @Override
    public void visit(AnnotationDeclaration n, Void arg) {
        String annotationName = n.getFullyQualifiedName().orElse(n.getNameAsString());
        graphData.addNode(annotationName, "annotation");
        super.visit(n, arg);
    }
}
