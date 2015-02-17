package main.java.com.jetbrains.term.expr;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.PrettyPrintable;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import main.java.com.jetbrains.term.visitor.*;

import java.util.List;
import java.util.Map;

public abstract class Expression implements PrettyPrintable {
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

    public final void checkType(List<Definition> context, Expression expected) throws TypeCheckingException {
        accept(new CheckTypeVisitor(context, expected));
    }

    public final Expression inferType(List<Definition> context) throws TypeCheckingException {
        return accept(new InferTypeVisitor(context));
    }
}