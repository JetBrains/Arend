package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Let;

public class LetExpression extends Expression {
  private final List<LetClause> myClauses;
  private final Expression myExpression;

  public LetExpression(List<LetClause> clauses, Expression expression) {
    myClauses = clauses;
    myExpression = expression;
  }

  public LetExpression mergeNestedLets() {
    List<LetClause> clauses = new ArrayList<>(myClauses);
    Expression expression = myExpression;
    while (expression.toLet() != null) {
      clauses.addAll(expression.toLet().getClauses());
      expression = expression.toLet().getExpression();
    }
    return Let(clauses, expression);
  }

  public List<LetClause> getClauses() {
    return myClauses;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLet(this, params);
  }

  @Override
  public LetExpression toLet() {
    return this;
  }

  private <T extends Binding> List<T> getBindingsFreeIn(List<T> bindings, Expression expr) {
    List<T> result = Collections.emptyList();
    for (T binding : bindings) {
      if (expr.findBinding(binding)) {
        if (result.isEmpty()) {
          result = new ArrayList<>(bindings.size());
        }
        result.add(binding);
      }
    }
    return result;
  }

  public TypeMax getType(TypeMax type) {
    if (type instanceof Expression) {
      return new LetExpression(myClauses, (Expression) type);
    } else {
      for (DependentLink link = type.getPiParameters(); link.hasNext(); link = link.getNext()) {
        List<LetClause> clauses = getBindingsFreeIn(myClauses, link.getType().toExpression());
        link.setType(clauses.isEmpty() ? link.getType() : new LetExpression(clauses, link.getType().toExpression()));
      }
      return type;
    }
  }

  @Override
  public Variable getStuckVariable() {
    return myExpression.getStuckVariable();
  }
}
