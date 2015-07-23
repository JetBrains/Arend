package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class LetExpression extends Expression implements Abstract.LetExpression {
    private final List<LetClause> myClauses;
    private final Expression myExpression;

    public LetExpression(List<LetClause> clauses, Expression expression) {
        myClauses = clauses;
        myExpression = expression;
    }

    @Override
    public List<LetClause> getClauses() {
        return myClauses;
    }

    @Override
    public Expression getExpression() {
        return myExpression;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
        return visitor.visitLet(this, params);
    }

    @Override
    public <T> T accept(ExpressionVisitor<? extends T> visitor) {
        return visitor.visitLet(this);
    }
}
