package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashMap;
import java.util.Map;

public class GoalError extends LocalTypeCheckingError {
  public final Map<Abstract.ReferableSourceNode, Binding> context;
  public final ExpectedType type;

  public GoalError(Map<Abstract.ReferableSourceNode, Binding> context, ExpectedType type, Abstract.Expression expression) {
    super(Level.GOAL, "Goal", expression);
    this.context = new HashMap<>(context);
    this.type = type;
  }
}
