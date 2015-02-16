package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.PrettyPrintable;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.typechecking.TypeMismatchException;
import main.java.com.jetbrains.term.visitor.*;

import java.util.List;
import java.util.Map;

public abstract class Expression implements PrettyPrintable {
    public abstract int precedence();
    public void checkType(List<Definition> context, Expression expected) throws TypeCheckingException {
        Expression actual = inferType(context);
        if (!expected.equals(actual)) {
            throw new TypeMismatchException(expected, actual, this);
        }
    }
    public abstract Expression inferType(List<Definition> context) throws TypeCheckingException;
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
    public final Expression fixVariables(List<String> names, Map<String, Definition> signature) {
        return accept(new FixVariablesVisitor(names, signature));
    }
}