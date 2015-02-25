package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingException;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeInferenceException;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeMismatchException;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.*;

public class InferTypeVisitor implements ExpressionVisitor<Expression> {
    private final List<Definition> context;

    public InferTypeVisitor(List<Definition> context) {
        this.context = context;
    }

    @Override
    public Expression visitApp(AppExpression expr) {
        if (expr.getFunction() instanceof NelimExpression) {
            Expression type = expr.getArgument().accept(this);
            return Pi(Pi(Nat(), Pi(type, type)), Pi(Nat(), type));
        }

        Expression functionType = expr.getFunction().accept(this).normalize();
        if (functionType instanceof PiExpression) {
            PiExpression piType = (PiExpression)functionType;
            expr.getArgument().checkType(context, piType.getLeft());
            return piType.getRight().subst(expr.getArgument(), 0);
        } else {
            throw new TypeMismatchException(Pi(Var("_"), Var("_")), functionType, expr.getFunction());
        }
    }

    @Override
    public Expression visitDefCall(DefCallExpression expr) {
        return expr.getDefinition().getSignature().getType();
    }

    @Override
    public Expression visitIndex(IndexExpression expr) {
        assert expr.getIndex() < context.size();
        return context.get(context.size() - 1 - expr.getIndex()).getSignature().getType().liftIndex(0, expr.getIndex() + 1);
    }

    @Override
    public Expression visitLam(LamExpression expr) {
        throw new TypeInferenceException(expr);
    }

    @Override
    public Expression visitNat(NatExpression expr) {
        return Universe(0);
    }

    @Override
    public Expression visitNelim(NelimExpression expr) {
        throw new TypeInferenceException(expr);
    }

    @Override
    public Expression visitPi(PiExpression expr) {
        Expression leftType = expr.getLeft().accept(this).normalize();
        // TODO: This is ugly. Fix it.
        context.add(new FunctionDefinition(expr.getVariable(), new Signature(expr.getLeft()), Var(expr.getVariable())));
        Expression rightType = expr.getRight().accept(this).normalize();
        context.remove(context.size() - 1);
        boolean leftOK = leftType instanceof UniverseExpression;
        boolean rightOK = rightType instanceof UniverseExpression;
        if (leftOK && rightOK) {
            return Universe(Math.max(((UniverseExpression) leftType).getLevel(), ((UniverseExpression) rightType).getLevel()));
        } else {
            throw new TypeMismatchException(Universe(-1), leftOK ? rightType : leftType, leftOK ? expr.getRight() : expr.getLeft());
        }
    }

    @Override
    public Expression visitSuc(SucExpression expr) {
        return Pi(Nat(), Nat());
    }

    @Override
    public Expression visitUniverse(UniverseExpression expr) {
        return Universe(expr.getLevel() == -1 ? -1 : expr.getLevel() + 1);
    }

    @Override
    public Expression visitVar(VarExpression expr) {
        throw new TypeCheckingException(expr);
    }

    @Override
    public Expression visitZero(ZeroExpression expr) {
        return Nat();
    }
}
