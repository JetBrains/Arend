package org.arend.extImpl;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.expr.AbstractedExpression;

public class AbstractedExpressionImpl implements AbstractedExpression {
  private final DependentLink myParameters;
  private final AbstractedExpression myExpression;

  private AbstractedExpressionImpl(DependentLink parameters, AbstractedExpression expression) {
    myParameters = parameters;
    myExpression = expression;
  }

  public static AbstractedExpression make(DependentLink parameters, AbstractedExpression expression) {
    return parameters.hasNext() ? new AbstractedExpressionImpl(parameters, expression) : expression;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public AbstractedExpression getExpression() {
    return myExpression;
  }

  public static AbstractedExpression subst(AbstractedExpression expression, SubstVisitor visitor) {
    if (visitor.isEmpty()) {
      return expression;
    }
    if (expression instanceof Expression) {
      return ((Expression) expression).accept(visitor, null);
    }
    if (expression instanceof AbstractedDependentLinkType) {
      AbstractedDependentLinkType abs = (AbstractedDependentLinkType) expression;
      return new AbstractedExpressionImpl(abs.getParameters().subst(visitor, abs.getSize(), false), DependentLink.Helper.get(abs.getParameters(), abs.getSize()).getTypeExpr().accept(visitor, null));
    }
    if (!(expression instanceof AbstractedExpressionImpl)) {
      throw new IllegalArgumentException();
    }
    AbstractedExpressionImpl abs = (AbstractedExpressionImpl) expression;
    return new AbstractedExpressionImpl(DependentLink.Helper.subst(abs.myParameters, visitor), subst(abs.myExpression, visitor));
  }
}
