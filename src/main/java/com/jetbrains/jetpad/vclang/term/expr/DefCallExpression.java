package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ReplaceDefCallVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefCallExpression extends Expression implements Abstract.DefCallExpression {
  private final Expression myExpression;
  private final ResolvedName myResolvedName;
  private List<Expression> myParameters;

  public DefCallExpression(Expression expression, ResolvedName resolvedName, List<Expression> parameters) {
    myExpression = expression;
    myResolvedName = resolvedName;
    myParameters = parameters;
  }

  public List<Expression> getParameters() {
    return myParameters;
  }

  public void setParameters(List<Expression> parameters) {
    myParameters = parameters;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public ResolvedName getResolvedName() {
    return myResolvedName;
  }

  public Definition getDefinition() {
    return myResolvedName.toDefinition();
  }

  @Override
  public void setResolvedName(ResolvedName name) {
    throw new IllegalStateException();
  }

  @Override
  public Name getName() {
    return myResolvedName.name;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitDefCall(this);
  }

  @Override
  public Expression getType(List<Binding> context) {
    Expression resultType;
    resultType = getDefinition().getType();

    if (getDefinition() instanceof Constructor && !((Constructor) getDefinition()).getDataType().getParameters().isEmpty()) {
      List<Expression> parameters = new ArrayList<>(myParameters);
      Collections.reverse(parameters);
      resultType = resultType.subst(parameters, 0);
    }
    return myExpression == null ? resultType : resultType.accept(new ReplaceDefCallVisitor(myResolvedName.namespace, myExpression));
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDefCall(this, params);
  }
}
