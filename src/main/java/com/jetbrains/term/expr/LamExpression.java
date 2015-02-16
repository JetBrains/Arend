package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.typechecking.TypeInferenceException;
import main.java.com.jetbrains.term.typechecking.TypeMismatchException;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

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
    public Expression fixVariables(List<String> names, Map<String, Definition> signature) throws NotInScopeException {
        names.add(variable);
        Expression body1 = body.fixVariables(names, signature);
        names.remove(names.size() - 1);
        return new LamExpression(variable, body1);
    }

    @Override
    public Expression normalize() {
        return new LamExpression(variable, body.normalize());
    }

    @Override
    public Expression subst(Expression expr, int from) {
        return new LamExpression(variable, body.subst(expr.liftIndex(0, 1), from + 1));
    }

    @Override
    public Expression liftIndex(int from, int on) {
        return new LamExpression(variable, body.liftIndex(from + 1, on));
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
    public void checkType(List<Definition> context, Expression expected) throws TypeCheckingException {
        Expression expectedNormalized = expected.normalize();
        if (expectedNormalized instanceof PiExpression) {
            PiExpression type = (PiExpression)expectedNormalized;
            context.add(new FunctionDefinition(variable, type.getLeft(), new VarExpression(variable)));
            body.checkType(context, type.getRight());
            context.remove(context.size() - 1);
        } else {
            throw new TypeMismatchException(expectedNormalized, new PiExpression(new VarExpression("_"), new VarExpression("_")), this);
        }
    }

    @Override
    public Expression inferType(List<Definition> context) throws TypeCheckingException {
        throw new TypeInferenceException(this);
    }
}
