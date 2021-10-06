package org.arend.naming.reference;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.core.expr.AbstractedExpression;
import org.arend.naming.renamer.Renamer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AbstractedReferable implements Referable {
  public final AbstractedExpression expression;
  public final List<? extends ConcreteExpression> arguments;

  public AbstractedReferable(AbstractedExpression expression, List<? extends ConcreteExpression> arguments) {
    this.expression = expression;
    this.arguments = arguments;
  }

  @Override
  public @NotNull String textRepresentation() {
    return Renamer.UNNAMED;
  }

  @Override
  public boolean isLocalRef() {
    return false;
  }
}
