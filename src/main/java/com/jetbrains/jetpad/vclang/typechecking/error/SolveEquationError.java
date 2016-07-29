package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;

public class SolveEquationError<E extends PrettyPrintable> extends TypeCheckingError {
  public final E expr1;
  public final E expr2;
  public final Binding binding;

  public SolveEquationError(Abstract.Definition definition, E expr1, E expr2, Binding binding, Abstract.SourceNode expression) {
    super(definition, "Cannot solve equation", expression);
    this.expr1 = expr1;
    this.expr2 = expr2;
    this.binding = binding;
  }

  @Deprecated
  public SolveEquationError(E expr1, E expr2, Binding binding, Abstract.SourceNode expression) {
    this(null, expr1, expr2, binding, expression);
  }
}
