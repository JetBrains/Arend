package com.jetbrains.jetpad.term.expr;

import com.jetbrains.jetpad.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class SucExpression extends Expression {
    public final static int PREC = 11;

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        stream.print(toString());
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof SucExpression;
    }

    @Override
    public String toString() {
        return "S";
    }

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitSuc(this);
    }
}
