package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils.ContextSaver;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Let;

public class LetExpression extends Expression implements Abstract.LetExpression {
  private final List<LetClause> myClauses;
  private final Expression myExpression;

  public LetExpression(List<LetClause> clauses, Expression expression) {
    myClauses = clauses;
    myExpression = expression;
  }

  public LetExpression mergeNestedLets() {
    List<LetClause> clauses = new ArrayList<>(myClauses);
    Expression expression = myExpression;
    while (expression instanceof  LetExpression) {
      clauses.addAll(((LetExpression) expression).getClauses());
      expression = ((LetExpression) expression).getExpression();
    }
    return Let(clauses, expression);
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
  public Expression getType(List<Binding> context) {
    try (ContextSaver ignore = new ContextSaver(context)) {
      context.addAll(myClauses);
      return myExpression.getType(context).normalize(NormalizeVisitor.Mode.NF, context).liftIndex(0, -myClauses.size());
    }
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLet(this, params);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLet(this, params);
  }
}
