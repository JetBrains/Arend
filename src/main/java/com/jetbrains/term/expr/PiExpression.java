package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class PiExpression extends Expression {
    private final String variable;
    private final Expression left;
    private final Expression right;

    public PiExpression(Expression left, Expression right) {
        this(null, left, right.liftIndex(0, 1));
    }

    public PiExpression(String variable, Expression left, Expression right) {
        this.variable = variable;
        this.left = left;
        this.right = right;
    }

    public String getVariable() {
        return variable;
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    @Override
    public int precedence() {
        return 6;
    }

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        if (prec > precedence()) stream.print("(");
        if (variable == null) {
            left.prettyPrint(stream, names, precedence() + 1);
        } else {
            stream.print("(" + variable + " : ");
            names.add(variable);
            left.prettyPrint(stream, names, 0);
            names.remove(names.size() - 1);
            stream.print(")");
        }
        stream.print(" -> ");
        right.prettyPrint(stream, names, precedence());
        if (prec > precedence()) stream.print(")");
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof PiExpression)) return false;
        PiExpression other = (PiExpression)o;
        return left.equals(other.left) && right.equals(other.right);
    }

    @Override
    public String toString() {
        return "(" + (variable == null ? "" : variable + " : ") + left.toString() + ") -> " + right.toString();
    }

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitPi(this);
    }
}
