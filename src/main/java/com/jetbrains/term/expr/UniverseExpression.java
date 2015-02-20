package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class UniverseExpression extends Expression {
    public final static int PREC = 11;

    private final int level;

    public UniverseExpression() {
        level = -1;
    }

    public UniverseExpression(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        stream.print(toString());
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof UniverseExpression;
    }

    @Override
    public String toString() {
        return "Type" + (level < 0 ? "" : level);
    }

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitUniverse(this);
    }
}
