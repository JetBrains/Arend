package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

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
    public Expression fixVariables(List<String> names, Map<String, Definition> signature) throws NotInScopeException {
        return this;
    }

    @Override
    public Expression normalize() {
        return definition.getTerm().normalize();
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
        if (!(o instanceof DefCallExpression)) return false;
        DefCallExpression other = (DefCallExpression)o;
        return definition.equals(other.definition);
    }

    @Override
    public String toString() {
        return definition.getName();
    }

    @Override
    public Expression inferType(List<Definition> context) throws TypeCheckingException {
        return definition.getType();
    }
}
