package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.PrettyPrintable;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.typechecking.TypeMismatchException;
import main.java.com.jetbrains.term.visitor.*;

import java.util.List;
import java.util.Map;

public abstract class Expression implements PrettyPrintable {
    private static final Expression NAT = new NatExpression();
    private static final Expression ZERO = new ZeroExpression();
    private static final Expression SUC = new SucExpression();
    private static final Expression NELIM = new NelimExpression();
    public abstract int precedence();
    public abstract <T> T accept(ExpressionVisitor<? extends T> visitor);

    public final Expression liftIndex(int from, int on) {
        return accept(new LiftIndexVisitor(from, on));
    }

    public final Expression subst(Expression substExpr, int from) {
        return accept(new SubstVisitor(substExpr, from));
    }

    public final Expression normalize() {
        return accept(new NormalizeVisitor());
    }

    public final Expression fixVariables(List<String> names, Map<String, Definition> signature) throws NotInScopeException {
        return accept(new FixVariablesVisitor(names, signature));
    }

    public final Expression inferType(List<Definition> context) throws TypeCheckingException {
        return accept(new InferTypeVisitor(context));
    }

    public void checkType(List<Definition> context, Expression expected) throws TypeCheckingException {
        Expression actualNorm = inferType(context).normalize();
        Expression expectedNorm = expected.normalize();
        if (!expectedNorm.equals(actualNorm)) {
            throw new TypeMismatchException(expectedNorm, actualNorm, this);
        }
    }

    public static Expression Apps(Expression expr, Expression... exprs) {
        for (Expression expr1 : exprs) {
            expr = new AppExpression(expr, expr1);
        }
        return expr;
    }

    public static Expression DefCall(Definition definition) {
        return new DefCallExpression(definition);
    }

    public static Expression Index(int i) {
        return new IndexExpression(i);
    }

    public static Expression Lam(String variable, Expression body) {
        return new LamExpression(variable, body);
    }

    public static Expression Pi(String variable, Expression left, Expression right) {
        return new PiExpression(variable, left, right);
    }

    public static Expression Pi(Expression left, Expression right) {
        return new PiExpression(left, right);
    }

    public static Expression Var(String name) {
        return new VarExpression(name);
    }

    public static Expression Nat() {
        return NAT;
    }

    public static Expression Zero() {
        return ZERO;
    }

    public static Expression Suc() {
        return SUC;
    }

    public static Expression Suc(Expression expr) {
        return Apps(SUC, expr);
    }

    public static Expression Universe(int level) {
        return new UniverseExpression(level);
    }

    public static Expression Nelim() {
        return NELIM;
    }
}