package org.arend.typechecking.error.local;

import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;

public class NotEnoughPatternsError extends TypecheckingError {
  public final int numberOfPatterns;

  public NotEnoughPatternsError(int numberOfPatterns, @Nonnull Concrete.SourceNode cause) {
    super("Not enough patterns, expected " + numberOfPatterns + " more", cause);
    assert numberOfPatterns > 0;
    this.numberOfPatterns = numberOfPatterns;
  }
}
