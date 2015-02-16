package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.PrettyPrintable;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.typechecking.TypeMismatchException;

import java.util.List;
import java.util.Map;

public abstract class Expression implements PrettyPrintable {
    public abstract int precedence();
    public abstract Expression fixVariables(List<String> names, Map<String, Definition> signature) throws NotInScopeException;
    // TODO: Rewrite normalization using thunks
    // TODO: Add normalization to whnf
    public abstract Expression normalize();
    public abstract Expression subst(Expression expr, int from);
    public abstract Expression liftIndex(int from, int on);
    public void checkType(List<Definition> context, Expression expected) throws TypeCheckingException {
        Expression actual = inferType(context);
        if (!expected.equals(actual)) {
            throw new TypeMismatchException(expected, actual, this);
        }
    }
    public abstract Expression inferType(List<Definition> context) throws TypeCheckingException;
}