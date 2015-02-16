package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class UniverseExpression extends Expression {
    @Override
    public int precedence() {
        return 11;
    }

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        stream.print(toString());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        return o instanceof UniverseExpression;
    }

    @Override
    public String toString() {
        return "Type";
    }

    @Override
    public Expression inferType(List<Definition> context) throws TypeCheckingException {
        return this;
    }

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitUniverse(this);
    }
}
