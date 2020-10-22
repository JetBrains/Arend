package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.Nullable;

public class MissingArgumentsError extends TypecheckingError {
  public final int numberOfArgs;

  public MissingArgumentsError(int numberOfArgs, @Nullable ConcreteSourceNode cause) {
    super("Required " + numberOfArgs + " more explicit argument" + (numberOfArgs == 1 ? "" : "s"), cause);
    this.numberOfArgs = numberOfArgs;
  }
}
