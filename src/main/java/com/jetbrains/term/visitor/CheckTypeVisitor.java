package main.java.com.jetbrains.term.visitor;

import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.expr.*;
import main.java.com.jetbrains.term.typechecking.TypeMismatchException;

import java.util.List;

public class CheckTypeVisitor implements ExpressionVisitor<Void> {
    private final List<Definition> context;
    private final Expression expected;

    public CheckTypeVisitor(List<Definition> context, Expression expected) {
        this.context = context;
        this.expected = expected;
    }

    private Void checkExpression(Expression expr) {
        Expression actual = expr.inferType(context);
        if (!expected.equals(actual)) {
            throw new TypeMismatchException(expected, actual, expr);
        }
        return null;
    }

    @Override
    public Void visitApp(AppExpression expr) {
        return checkExpression(expr);
    }

    @Override
    public Void visitDefCall(DefCallExpression expr) {
        return checkExpression(expr);
    }

    @Override
    public Void visitIndex(IndexExpression expr) {
        return checkExpression(expr);
    }

    @Override
    public Void visitLam(LamExpression expr) {
        Expression expectedNormalized = expected.normalize();
        if (expectedNormalized instanceof PiExpression) {
            PiExpression type = (PiExpression)expectedNormalized;
            context.add(new FunctionDefinition(expr.getVariable(), type.getLeft(), new VarExpression(expr.getVariable())));
            expr.getBody().checkType(context, type.getRight());
            context.remove(context.size() - 1);
            return null;
        } else {
            throw new TypeMismatchException(expectedNormalized, new PiExpression(new VarExpression("_"), new VarExpression("_")), expr);
        }
    }

    @Override
    public Void visitNat(NatExpression expr) {
        return checkExpression(expr);
    }

    @Override
    public Void visitNelim(NelimExpression expr) {
        return checkExpression(expr);
    }

    @Override
    public Void visitPi(PiExpression expr) {
        return checkExpression(expr);
    }

    @Override
    public Void visitSuc(SucExpression expr) {
        return checkExpression(expr);
    }

    @Override
    public Void visitUniverse(UniverseExpression expr) {
        return checkExpression(expr);
    }

    @Override
    public Void visitVar(VarExpression expr) {
        return checkExpression(expr);
    }

    @Override
    public Void visitZero(ZeroExpression expr) {
        return checkExpression(expr);
    }
}
