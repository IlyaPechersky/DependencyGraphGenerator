package com.parser.visitors;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.parser.model.GraphData;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public abstract class BaseVisitor extends VoidVisitorAdapter<Void> {
    protected final GraphData graphData;
    protected final JavaSymbolSolver symbolSolver;

    public BaseVisitor(GraphData graphData, JavaSymbolSolver symbolSolver) {
        this.graphData = graphData;
        this.symbolSolver = symbolSolver;
    }

    protected String resolveFullTypeName(Type type) {
        try {
            return type.resolve().describe();
        } catch (Exception e) {
            System.err.println("Failed to resolve type: " + type + ". Error: " + e.getMessage());
            return "unresolved." + type.toString().replaceAll("[^a-zA-Z0-9]", "_");
        }
    }

    protected String resolveAnnotationName(AnnotationExpr annotation) {
        try {
            ResolvedAnnotationDeclaration resolved = annotation.resolve();
            return resolved.getQualifiedName();
        } catch (Exception e) {
            System.err.println("Failed to resolve annotation: " + annotation.getName() + ". Error: " + e.getMessage());
            return annotation.getNameAsString();
        }
    }
}
