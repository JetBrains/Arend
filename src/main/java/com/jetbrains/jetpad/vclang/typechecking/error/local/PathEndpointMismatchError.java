package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class PathEndpointMismatchError extends LocalTypeCheckingError {
  public final boolean isLeft;
  public final Expression expected;
  public final Expression actual;

  public PathEndpointMismatchError(boolean isLeft, Expression expected, Expression actual, Abstract.SourceNode cause) {
    super("The " + (isLeft ? "left" : "right") + " path endpoint mismatch", cause);
    this.isLeft = isLeft;
    this.expected = expected;
    this.actual = actual;
  }
}
