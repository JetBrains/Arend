package org.arend.ext.typechecking;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Defines a few utility methods that can be used to check the validity of arguments and the expected type.
 */
public abstract class BaseMetaDefinition extends ContextDataChecker implements MetaDefinition {
  @Override
  public boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments) {
    return checkArguments(arguments, null, null, argumentExplicitness());
  }
}
