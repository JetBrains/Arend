package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

public class IndexExpression extends Expression {
    private final int index;

    public IndexExpression(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public int precedence() {
        return 11;
    }

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        assert index < names.size();
        stream.print(names.get(names.size() - 1 - index));
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
        if (index < from) return this;
        if (index == from) return expr; // .liftIndex(0, from);
        return new IndexExpression(index - 1);
    }

    @Override
    public Expression liftIndex(int from, int on) {
        if (index < from) return this;
        else return new IndexExpression(index + on);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof IndexExpression)) return false;
        IndexExpression other = (IndexExpression)o;
        return index == other.index;
    }

    @Override
    public String toString() {
        return "<" + index + ">";
    }

    @Override
    public Expression inferType(List<Definition> context) throws TypeCheckingException {
        assert index < context.size();
        return context.get(context.size() - 1 - index).getType();
    }
}
