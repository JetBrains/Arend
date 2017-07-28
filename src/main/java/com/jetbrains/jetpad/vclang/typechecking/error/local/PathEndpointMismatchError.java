package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.Abstract;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

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

  @Override
  public Doc getBodyDoc() {
    return vList(
      hang(text("Expected:"), termDoc(expected)),
      hang(text("  Actual:"), termDoc(actual)));
  }
}
