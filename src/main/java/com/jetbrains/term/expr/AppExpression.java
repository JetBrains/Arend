package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.typechecking.TypeMismatchException;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

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
    public Expression fixVariables(List<String> names, Map<String, Definition> signature) throws NotInScopeException {
        return new AppExpression(function.fixVariables(names, signature), argument.fixVariables(names, signature));
    }

    @Override
    public Expression normalize() {
        Expression function1 = function.normalize();
        if (function1 instanceof LamExpression) {
            Expression body = ((LamExpression)function1).getBody();
            return body.subst(argument, 0).normalize();
        }
        if (function1 instanceof AppExpression) {
            AppExpression appExpr1 = (AppExpression)function1;
            if (appExpr1.function instanceof AppExpression) {
                AppExpression appExpr2 = (AppExpression)appExpr1.function;
                if (appExpr2.function instanceof NelimExpression) {
                    Expression zeroClause = appExpr2.argument;
                    Expression sucClause = appExpr1.argument;
                    Expression caseExpr = argument.normalize();
                    if (caseExpr instanceof ZeroExpression) return zeroClause;
                    if (caseExpr instanceof AppExpression) {
                        AppExpression appExpr3 = (AppExpression)caseExpr;
                        if (appExpr3.function instanceof SucExpression) {
                            Expression recursiveCall = new AppExpression(appExpr1, appExpr3.argument);
                            Expression result = new AppExpression(new AppExpression(sucClause, appExpr3.argument), recursiveCall);
                            return result.normalize();
                        }
                    }
                }
            }
        }
        return new AppExpression(function1, argument.normalize());
    }

    @Override
    public Expression subst(Expression expr, int from) {
        return new AppExpression(function.subst(expr, from), argument.subst(expr, from));
    }

    @Override
    public Expression liftIndex(int from, int on) {
        return new AppExpression(function.liftIndex(from, on), argument.liftIndex(from, on));
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

    private Expression checkNelim(List<Definition> context) throws TypeCheckingException {
        if (!(function instanceof NelimExpression)) return null;
        Expression type = argument.inferType(context);
        return new PiExpression(new PiExpression(new NatExpression(), new PiExpression(type, type)), new PiExpression(new NatExpression(), type));
    }
}
