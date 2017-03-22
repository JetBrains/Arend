package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;

public class GoalError extends LocalTypeCheckingError {
  public final List<Binding> context;
  public final ExpectedType type;

  public GoalError(List<Binding> context, ExpectedType type, Abstract.Expression expression) {
    super(Level.GOAL, "Goal", expression);
    this.context = new ArrayList<>(context);
    this.type = type;
  }
}
