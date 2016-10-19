package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

import java.util.ArrayList;
import java.util.List;

public class GoalError extends LocalTypeCheckingError {
  public final List<Binding> context;
  public final Type type;

  public GoalError(List<Binding> context, Type type, Abstract.Expression expression) {
    super(Level.GOAL, "Goal", expression);
    this.context = new ArrayList<>(context);
    this.type = type;
  }
}
