package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.List;

public class ConCallExpression extends DefCallExpression {
  private List<Expression> myDataTypeArguments;

  public ConCallExpression(Constructor definition, List<Expression> dataTypeArguments) {
    super(definition);
    assert dataTypeArguments != null;
    myDataTypeArguments = dataTypeArguments;
  }

  public List<? extends Expression> getDataTypeArguments() {
    return myDataTypeArguments;
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    assert myDataTypeArguments.isEmpty();
    myDataTypeArguments.add(thisExpr);
    return this;
  }

  @Override
  public Constructor getDefinition() {
    return (Constructor) super.getDefinition();
  }

  @Override
  public ConCallExpression applyLevelSubst(LevelSubstitution subst) {
    super.applyLevelSubst(subst);
    List<Expression> newArgs = new ArrayList<>(myDataTypeArguments.size());
    for (Expression arg : myDataTypeArguments) {
      newArgs.add(arg.subst(subst));
    }
    myDataTypeArguments = newArgs;
    return this;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConCall(this, params);
  }

  @Override
  public ConCallExpression toConCall() {
    return this;
  }
}
