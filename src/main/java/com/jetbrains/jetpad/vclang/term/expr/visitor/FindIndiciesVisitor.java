package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.*;

public class FindIndiciesVisitor implements ExpressionVisitor<Set<Integer>> {

    private final Set<Integer> myResult = new HashSet<>();
    private final int myFrom;
    private final int myTo;
    private int myLift;

    private class Saver implements AutoCloseable {
        final int myOldLift = myLift;

        @Override
        public void close() {
            myLift = myOldLift;
        }
    }

    private void lift(int lift) {
        myLift += lift;
    }

    public FindIndiciesVisitor(int from, int to) {
        myFrom = from;
        myTo = to;
    }

    @Override
    public Set<Integer> visitApp(AppExpression expr) {
        expr.getFunction().accept(this);
        return expr.getArgument().getExpression().accept(this);
    }

    @Override
    public Set<Integer> visitDefCall(DefCallExpression expr) {
        return myResult;
    }

    @Override
    public Set<Integer> visitIndex(IndexExpression expr) {
        if (expr.getIndex() >= myFrom + myLift && expr.getIndex() <= myTo + myLift) {
            myResult.add(expr.getIndex() - myLift);
        }
        return myResult;
    }

    Set<Integer> visitArguments(List<? extends Argument> arguments) {
        for (Argument arg : arguments) {
            if (arg instanceof TelescopeArgument) {
                ((TelescopeArgument) arg).getType().accept(this);
                lift(((TelescopeArgument) arg).getNames().size());
            } else if (arg instanceof NameArgument) {
                lift(1);
            } else if (arg instanceof TypeArgument) {
                ((TypeArgument) arg).getType().accept(this);
                lift(1);
            }
        }
        return myResult;
    }

    @Override
    public Set<Integer> visitLam(LamExpression expr) {
        try (Saver saver = new Saver()) {
            visitArguments(expr.getArguments());
            expr.getBody().accept(this);
        }
        return myResult;
    }

    @Override
    public Set<Integer> visitPi(PiExpression expr) {
        try (Saver saver = new Saver()) {
            visitArguments(expr.getArguments());
            expr.getCodomain().accept(this);
        }
        return myResult;
    }

    @Override
    public Set<Integer> visitUniverse(UniverseExpression expr) {
        return myResult;
    }

    @Override
    public Set<Integer> visitVar(VarExpression expr) {
        return myResult;
    }

    @Override
    public Set<Integer> visitInferHole(InferHoleExpression expr) {
        return myResult;
    }

    @Override
    public Set<Integer> visitError(ErrorExpression expr) {
        return myResult;
    }

    @Override
    public Set<Integer> visitTuple(TupleExpression expr) {
        for (int i = 1; i < expr.getFields().size(); i++) {
            myResult.addAll(expr.getFields().get(i).accept(this));
        }
        return myResult;
    }

    @Override
    public Set<Integer> visitSigma(SigmaExpression expr) {
        try (Saver saver = new Saver()){
            return visitArguments(expr.getArguments());
        }
    }

    @Override
    public Set<Integer> visitElim(ElimExpression expr) {
        try (Saver saver = new Saver()) {
            expr.getExpression().accept(this);
            for (Clause clause : expr.getClauses()) {
                try (Saver clauseSaver = new Saver()) {
                    visitArguments(clause.getArguments());
                    clause.getExpression().accept(this);
                }
            }
            visitArguments(expr.getOtherwise().getArguments());
            expr.getOtherwise().getExpression().accept(this);
        }
        return myResult;
    }

    @Override
    public Set<Integer> visitFieldAcc(FieldAccExpression expr) {
        return expr.getExpression().accept(this);
    }

    @Override
    public Set<Integer> visitProj(ProjExpression expr) {
        return expr.getExpression().accept(this);
    }

    @Override
    public Set<Integer> visitClassExt(ClassExtExpression expr) {
        throw new IllegalStateException(); // TODO: fix
    }

    @Override
    public Set<Integer> visitNew(NewExpression expr) {
        return expr.getExpression().accept(this);
    }

    public Set<Integer> visitLetClause(LetClause clause) {
        try (Saver saver = new Saver()) {
            visitArguments(clause.getArguments());
            if (clause.getResultType() != null) clause.getResultType().accept(this);
            clause.getTerm().accept(this);
        }
        return myResult;
    }

    @Override
    public Set<Integer> visitLet(LetExpression letExpression) {
        try (Saver saver = new Saver()) {
            for (LetClause clause : letExpression.getClauses()) {
                visitLetClause(clause);
                lift(1);
            }
            letExpression.getExpression().accept(this);
        }
        return myResult;
    }
}
