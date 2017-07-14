package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;

public class PathEndpointMismatchError extends LocalTypeCheckingError {
  public final boolean isLeft;
  public final PrettyPrintable expected;
  public final PrettyPrintable actual;

  public PathEndpointMismatchError(boolean isLeft, PrettyPrintable expected, PrettyPrintable actual, Abstract.SourceNode cause) {
    super("The " + (isLeft ? "left" : "right") + " path endpoint mismatch", cause);
    this.isLeft = isLeft;
    this.expected = expected;
    this.actual = actual;
  }
}
