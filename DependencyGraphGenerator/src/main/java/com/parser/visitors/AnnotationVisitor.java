package com.parser.visitors;

import com.parser.model.GraphData;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class AnnotationVisitor extends VoidVisitorAdapter<Void> {
    private final GraphData graphData;

    public AnnotationVisitor(GraphData graphData) {
        this.graphData = graphData;
    }

    @Override
    public void visit(AnnotationDeclaration n, Void arg) {
        String annotationName = n.getFullyQualifiedName().orElse(n.getNameAsString());
        graphData.addNode(annotationName, "annotation");

        super.visit(n, arg);
    }
}
