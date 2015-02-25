package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class VarExpression extends Expression {
    public final static int PREC = 11;

    private final String name;

    public VarExpression(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitVar(this);
    }
}
