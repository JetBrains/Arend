package org.arend.extImpl;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.expr.AbstractedExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

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

  @Override
  public int getNumberOfAbstractedBindings() {
    return mySize;
  }

  @Override
  public @Nullable CoreBinding findFreeBinding(@NotNull Set<? extends CoreBinding> bindings) {
    if (bindings.isEmpty()) return null;
    DependentLink link = myParameters;
    for (int i = 0; i < mySize; i++) {
      while (!(link instanceof TypedDependentLink)) {
        link = link.getNext();
        i++;
      }
      if (i >= mySize) break;
      CoreBinding binding = link.getTypeExpr().findFreeBinding(bindings);
      if (binding != null) return binding;
    }
    return link.getTypeExpr().findFreeBinding(bindings);
  }
}
