package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class SolveEquationsError extends TypeCheckingError {
  public final Expression expr1;
  public final Expression expr2;
  public final Binding binding;

  public SolveEquationsError(Abstract.Definition definition, Expression expr1, Expression expr2, Binding binding, Abstract.SourceNode expression) {
    super(definition, "Cannot solve equation", expression);
    this.expr1 = expr1;
    this.expr2 = expr2;
    this.binding = binding;
  }

  @Deprecated
  public SolveEquationsError(Expression expr1, Expression expr2, Binding binding, Abstract.SourceNode expression) {
    this(null, expr1, expr2, binding, expression);
  }
}
