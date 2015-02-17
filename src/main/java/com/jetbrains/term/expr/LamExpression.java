package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class LamExpression extends Expression {
    private final String variable;
    private final Expression body;

    public LamExpression(String variable, Expression expression) {
        this.variable = variable;
        this.body = expression;
    }

    public String getVariable() {
        return variable;
    }

    public Expression getBody() {
        return body;
    }

    @Override
    public int precedence() {
        return 5;
    }

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        if (prec > precedence()) stream.print("(");
        String var;
        for (var = variable; names.contains(var); var += "'");
        stream.print("\\" + var + " -> ");
        names.add(var);
        body.prettyPrint(stream, names, precedence());
        names.remove(names.size() - 1);
        if (prec > precedence()) stream.print(")");
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LamExpression)) return false;
        LamExpression other = (LamExpression)o;
        return body.equals(other.body);
    }

    @Override
    public String toString() {
        return "\\" + variable + " -> " + body;
    }

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitLam(this);
    }
}
