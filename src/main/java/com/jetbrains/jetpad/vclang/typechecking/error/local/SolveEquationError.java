package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;

public class SolveEquationError<E extends PrettyPrintable> extends LocalTypeCheckingError {
  public final E expr1;
  public final E expr2;
  public final Binding binding;

  public SolveEquationError(E expr1, E expr2, Binding binding, Abstract.SourceNode expression) {
    super("Cannot solve equation", expression);
    this.expr1 = expr1;
    this.expr2 = expr2;
    this.binding = binding;
  }
}
