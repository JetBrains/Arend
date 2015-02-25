package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class ZeroExpression extends Expression {
    public final static int PREC = 11;

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        stream.print(toString());
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ZeroExpression;
    }

    @Override
    public String toString() {
        return "0";
    }

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitZero(this);
    }
}
