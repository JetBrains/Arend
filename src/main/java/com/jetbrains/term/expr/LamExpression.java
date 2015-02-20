package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.definition.Argument;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.typechecking.TypeMismatchException;
import main.java.com.jetbrains.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class LamExpression extends Expression {
    public final static int PREC = 5;

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
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        if (prec > PREC) stream.print("(");
        String var;
        for (var = variable; names.contains(var); var += "'");
        stream.print("\\" + var + " -> ");
        names.add(var);
        body.prettyPrint(stream, names, PREC);
        names.remove(names.size() - 1);
        if (prec > PREC) stream.print(")");
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

    @Override
    public void checkType(List<Definition> context, Expression expected) throws TypeCheckingException {
        Expression expectedNormalized = expected.normalize();
        if (expectedNormalized instanceof PiExpression) {
            PiExpression type = (PiExpression)expectedNormalized;
            // TODO: This is ugly. Fix it.
            context.add(new FunctionDefinition(variable, new Argument[0], type.getLeft(), new VarExpression(variable)));
            body.checkType(context, type.getRight());
            context.remove(context.size() - 1);
        } else
            throw new TypeMismatchException(expectedNormalized, new PiExpression(new VarExpression("_"), new VarExpression("_")), this);
    }
}
