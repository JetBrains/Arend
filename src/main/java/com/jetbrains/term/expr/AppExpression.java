package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.typechecking.TypeMismatchException;
import main.java.com.jetbrains.term.visitor.ExpressionVisitor;

import java.io.PrintStream;
import java.util.List;

public class AppExpression extends Expression {
    private final Expression function;
    private final Expression argument;

    public AppExpression(Expression function, Expression argument) {
        this.function = function;
        this.argument = argument;
    }

    public Expression getFunction() {
        return function;
    }

    public Expression getArgument() {
        return argument;
    }

    @Override
    public int precedence() {
        return 10;
    }

    @Override
    public void prettyPrint(PrintStream stream, List<String> names, int prec) {
        if (prec > precedence()) stream.print("(");
        function.prettyPrint(stream, names, precedence());
        stream.print(" ");
        argument.prettyPrint(stream, names, precedence() + 1);
        if (prec > precedence()) stream.print(")");
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof AppExpression)) return false;
        AppExpression other = (AppExpression)o;
        return function.equals(other.function) && argument.equals(other.argument);
    }

    @Override
    public String toString() {
        return function.toString() + " (" + argument.toString() + ")";
    }

    @Override
    public Expression inferType(List<Definition> context) throws TypeCheckingException {
        Expression nelimResult = checkNelim(context);
        if (nelimResult != null) return nelimResult;

        Expression functionType = function.inferType(context).normalize();
        if (functionType instanceof PiExpression) {
            PiExpression arrType = (PiExpression)functionType;
            argument.checkType(context, arrType.getLeft());
            return arrType.getRight();
        } else {
            throw new TypeMismatchException(new PiExpression(new VarExpression("_"), new VarExpression("_")), functionType, function);
        }
    }

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitApp(this);
    }

    private Expression checkNelim(List<Definition> context) throws TypeCheckingException {
        if (!(function instanceof NelimExpression)) return null;
        Expression type = argument.inferType(context);
        return new PiExpression(new PiExpression(new NatExpression(), new PiExpression(type, type)), new PiExpression(new NatExpression(), type));
    }
}
