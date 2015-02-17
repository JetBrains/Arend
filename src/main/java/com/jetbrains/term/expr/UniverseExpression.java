package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class UniverseExpression extends Expression {
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
        return o instanceof UniverseExpression;
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
