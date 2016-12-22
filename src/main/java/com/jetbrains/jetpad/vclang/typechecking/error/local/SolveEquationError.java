package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class SolveEquationError<E extends PrettyPrintable> extends LocalTypeCheckingError {
  public final E expr1;
  public final E expr2;

  public SolveEquationError(E expr1, E expr2, Abstract.SourceNode expression) {
    super("Cannot solve equation", expression);
    this.expr1 = expr1;
    this.expr2 = expr2;
  }
}
