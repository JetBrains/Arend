package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class SucExpression extends Expression {
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
        return o instanceof SucExpression;
    }

    @Override
    public String toString() {
        return "S";
    }

    @Override
    public Expression inferType(List<Definition> context) throws TypeCheckingException {
        return new PiExpression(new NatExpression(), new NatExpression());
    }

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitSuc(this);
    }
}
