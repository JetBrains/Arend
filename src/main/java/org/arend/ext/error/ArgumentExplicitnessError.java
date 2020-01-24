package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;

import javax.annotation.Nonnull;

public class ArgumentExplicitnessError extends TypecheckingError {
  public ArgumentExplicitnessError(boolean explicit, @Nonnull ConcreteSourceNode cause) {
    super("Expected an " + (explicit ? "explicit" : "implicit") + " argument" , cause);
  }
}
