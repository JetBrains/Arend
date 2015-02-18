package main.java.com.jetbrains.term.visitor;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.expr.*;

import java.util.List;
import java.util.Map;

public class FixVariablesVisitor implements ExpressionVisitor<Expression> {
    private List<String> names;
    private Map<String, Definition> signature;

    public FixVariablesVisitor(List<String> names, Map<String, Definition> signature) {
        this.names = names;
        this.signature = signature;
    }

    @Override
    public Expression visitApp(AppExpression expr) {
        return new AppExpression(expr.getFunction().accept(this), expr.getArgument().accept(this));
    }

    @Override
    public Expression visitDefCall(DefCallExpression expr) {
        return expr;
    }

    @Override
    public Expression visitIndex(IndexExpression expr) {
        return expr;
    }

    @Override
    public Expression visitLam(LamExpression expr) {
        names.add(expr.getVariable());
        Expression body1 = expr.getBody().accept(this);
        names.remove(names.size() - 1);
        return new LamExpression(expr.getVariable(), body1);
    }

    @Override
    public Expression visitNat(NatExpression expr) {
        return expr;
    }

    @Override
    public Expression visitNelim(NelimExpression expr) {
        return expr;
    }

    @Override
    public Expression visitPi(PiExpression expr) {
        names.add(expr.getVariable());
        Expression right1 = expr.getRight().accept(this);
        names.remove(names.size() - 1);
        return new PiExpression(expr.getVariable(), expr.getLeft().accept(this), right1);
    }

    @Override
    public Expression visitSuc(SucExpression expr) {
        return expr;
    }

    @Override
    public Expression visitUniverse(UniverseExpression expr) {
        return expr;
    }

    @Override
    public Expression visitVar(VarExpression expr) {
        int index = names.lastIndexOf(expr.getName());
        if (index == -1) {
            Definition def = signature.get(expr.getName());
            if (def == null) {
                throw new NotInScopeException(expr.getName());
            } else {
                return new DefCallExpression(def);
            }
        } else {
            return new IndexExpression(names.size() - 1 - index);
        }
    }

    @Override
    public Expression visitZero(ZeroExpression expr) {
        return expr;
    }
}
