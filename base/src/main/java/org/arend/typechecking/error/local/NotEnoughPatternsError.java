package org.arend.typechecking.error.local;

import org.arend.ext.error.TypecheckingError;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

public class NotEnoughPatternsError extends TypecheckingError {
  public final int numberOfPatterns;

  public NotEnoughPatternsError(int numberOfPatterns, @NotNull Concrete.SourceNode cause) {
    super("Not enough patterns, expected " + numberOfPatterns + " more", cause);
    assert numberOfPatterns > 0;
    this.numberOfPatterns = numberOfPatterns;
  }
}
