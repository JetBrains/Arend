package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;

public class ArgumentExplicitnessError extends TypecheckingError {
  public ArgumentExplicitnessError(boolean explicit, @NotNull ConcreteSourceNode cause) {
    super("Expected an " + (explicit ? "explicit" : "implicit") + " argument" , cause);
  }
}
