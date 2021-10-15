package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.quickFix.ErrorQuickFix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TypecheckingError extends LocalError {
  public ConcreteSourceNode cause;
  public final List<ErrorQuickFix> quickFixes = new ArrayList<>();

  public TypecheckingError(@NotNull Level level, @NotNull String message, @Nullable ConcreteSourceNode cause) {
    super(level, message);
    this.cause = cause;
  }

  public TypecheckingError(@NotNull String message, @Nullable ConcreteSourceNode cause) {
    super(Level.ERROR, message);
    this.cause = cause;
  }

  public TypecheckingError withQuickFix(ErrorQuickFix quickFix) {
    quickFixes.add(quickFix);
    return this;
  }

  @Override
  public ConcreteSourceNode getCauseSourceNode() {
    return cause;
  }

  @NotNull
  @Override
  public Stage getStage() {
    return Stage.TYPECHECKER;
  }
}
