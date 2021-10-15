package org.arend.ext.error.quickFix;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemoveErrorQuickFix implements ErrorQuickFix {
  private final String message;

  public RemoveErrorQuickFix(String message) {
    this.message = message;
  }

  @Override
  public @NotNull String getMessage() {
    return message;
  }

  @Override
  public @Nullable ConcreteSourceNode getReplacement() {
    return null;
  }
}
