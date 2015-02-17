package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class DefCallExpression extends Expression {
    private final Definition definition;

    public DefCallExpression(Definition function) {
        this.definition = function;
    }

    public Definition getDefinition() {
        return definition;
    }

    @Override
    public int precedence() {
        return 11;
    }

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        stream.print(definition.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof DefCallExpression)) return false;
        DefCallExpression other = (DefCallExpression)o;
        return definition.equals(other.definition);
    }

    @Override
    public String toString() {
        return definition.getName();
    }

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitDefCall(this);
    }
}
