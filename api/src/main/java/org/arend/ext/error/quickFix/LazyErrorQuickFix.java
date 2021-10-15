package org.arend.ext.error.quickFix;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LazyErrorQuickFix implements ErrorQuickFix {
  private final ErrorQuickFix quickFix;
  private ConcreteSourceNode sourceNode;
  private boolean computed;

  public LazyErrorQuickFix(ErrorQuickFix quickFix) {
    this.quickFix = quickFix;
  }

  @Override
  public @NotNull String getMessage() {
    return quickFix.getMessage();
  }

  @Override
  public @Nullable ConcreteSourceNode getReplacement() {
    if (!computed) {
      sourceNode = quickFix.getReplacement();
      computed = true;
    }
    return sourceNode;
  }
}
