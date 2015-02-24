package main.java.com.jetbrains.term.definition;

import main.java.com.jetbrains.term.expr.Expression;
import main.java.com.jetbrains.term.expr.PiExpression;

import java.util.ArrayList;
import java.util.List;

public class Signature {
    private final Argument[] arguments;
    private final Expression resultType;

    public Signature(Argument[] arguments, Expression resultType) {
        this.arguments = arguments;
        this.resultType = resultType;
    }

    public Signature(Expression type) {
        List<Argument> args = new ArrayList<Argument>();
        type = type.normalize();
        while (type instanceof PiExpression) {
            PiExpression pi = (PiExpression)type;
            args.add(new Argument(pi.isExplicit(), pi.getVariable(), pi.getLeft()));
            type = pi.getRight();
        }
        arguments = args.toArray(new Argument[args.size()]);
        resultType = type;
    }

    public Argument[] getArguments() {
        return arguments;
    }

    public Argument getArgument(int i) {
        return arguments[i];
    }

    public Expression getResultType() {
        return resultType;
    }

    public Expression getType() {
        Expression type = resultType;
        for (int i = arguments.length - 1; i >= 0; --i) {
            type = new PiExpression(arguments[i].isExplicit(), arguments[i].getName(), arguments[i].getType(), type);
        }
        return type;
    }

    @Override
    public String toString() {
        return getType().toString();
    }
}
