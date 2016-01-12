package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.param.Binding;
import com.jetbrains.jetpad.vclang.term.expr.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConCallExpression extends DefCallExpression {
  private List<Expression> myParameters;

  public ConCallExpression(Constructor definition, List<Expression> parameters) {
    super(definition);
    assert parameters != null;
    myParameters = parameters;
  }

  public List<Expression> getParameters() {
    return myParameters;
  }

  public void setParameters(List<Expression> parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    assert myParameters.isEmpty();
    myParameters.add(thisExpr);
    return this;
  }

  @Override
  public Constructor getDefinition() {
    return (Constructor) super.getDefinition();
  }

  @Override
  public Expression getType(List<Binding> context) {
    Expression resultType = super.getType(context);

    if (!myParameters.isEmpty()) {
      Map<Binding, Expression> substs = new HashMap<>();
      DependentLink link = getDefinition().getDataType().getParameters();
      for (Expression parameter : myParameters) {
        if (link == null) {
          return null;
        }
        substs.put(link, parameter);
        link = link.getNext();
      }
      resultType = resultType.subst(substs);
    }
    return resultType;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConCall(this, params);
  }
}
