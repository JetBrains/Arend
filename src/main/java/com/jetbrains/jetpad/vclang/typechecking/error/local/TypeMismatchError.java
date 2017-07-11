package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;

public class TypeMismatchError extends LocalTypeCheckingError {
  public final PrettyPrintable expected;
  public final PrettyPrintable actual;

  public TypeMismatchError(PrettyPrintable expected, PrettyPrintable actual, Abstract.Expression expression) {
    super("Type mismatch", expression);
    this.expected = expected;
    this.actual = actual;
  }
}
