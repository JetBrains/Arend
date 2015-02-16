package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

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
    public Expression fixVariables(List<String> names, Map<String, Definition> signature) throws NotInScopeException {
        return this;
    }

    @Override
    public Expression normalize() {
        return this;
    }

    @Override
    public Expression subst(Expression expr, int from) {
        return this;
    }

    @Override
    public Expression liftIndex(int from, int on) {
        return this;
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
}
