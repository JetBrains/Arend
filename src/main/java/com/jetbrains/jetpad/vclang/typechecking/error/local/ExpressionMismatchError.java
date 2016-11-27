package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.parser.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class ExpressionMismatchError extends LocalTypeCheckingError {
  public final PrettyPrintable expected;
  public final PrettyPrintable actual;

  public ExpressionMismatchError(PrettyPrintable expected, PrettyPrintable actual, Abstract.SourceNode cause) {
    super("Expressions are not equal", cause);
    this.expected = expected;
    this.actual = actual;
  }
}
