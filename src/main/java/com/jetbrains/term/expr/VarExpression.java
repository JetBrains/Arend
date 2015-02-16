package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

public class VarExpression extends Expression {
    private final String name;

    public VarExpression(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int precedence() {
        return 11;
    }

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        stream.print(name);
    }

    @Override
    public Expression fixVariables(List<String> names, Map<String, Definition> signature) throws NotInScopeException {
        int index = names.lastIndexOf(name);
        if (index == -1) {
            Definition def = signature.get(name);
            if (def == null) {
                throw new NotInScopeException(name);
            } else {
                return new DefCallExpression(def);
            }
        } else {
            return new IndexExpression(names.size() - 1 - index);
        }
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
        if (!(o instanceof VarExpression)) return false;
        VarExpression other = (VarExpression)o;
        return name.equals(other.name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Expression inferType(List<Definition> context) throws TypeCheckingException {
        throw new TypeCheckingException(this);
    }
}
