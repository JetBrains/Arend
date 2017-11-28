package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class PathEndpointMismatchError extends TypecheckingError {
  public final boolean isLeft;
  public final Expression expected;
  public final Expression actual;

  public PathEndpointMismatchError(boolean isLeft, Expression expected, Expression actual, Concrete.SourceNode cause) {
    super("The " + (isLeft ? "left" : "right") + " path endpoint mismatch", cause);
    this.isLeft = isLeft;
    this.expected = expected;
    this.actual = actual;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      hang(text("Expected:"), termDoc(expected, ppConfig)),
      hang(text("  Actual:"), termDoc(actual, ppConfig)));
  }
}
