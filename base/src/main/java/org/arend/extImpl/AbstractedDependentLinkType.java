package org.arend.extImpl;

import org.arend.core.context.param.DependentLink;
import org.arend.ext.core.expr.AbstractedExpression;

public class AbstractedDependentLinkType implements AbstractedExpression {
  private final DependentLink myParameters;
  private final int mySize;

  private AbstractedDependentLinkType(DependentLink parameters, int size) {
    myParameters = parameters;
    mySize = size;
  }

  public static AbstractedExpression make(DependentLink parameters, int size) {
    return size == 0 ? parameters.getTypeExpr() : new AbstractedDependentLinkType(parameters, size);
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public int getSize() {
    return mySize;
  }
}
