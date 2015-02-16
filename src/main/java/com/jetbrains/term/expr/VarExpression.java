package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

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

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitVar(this);
    }
}
