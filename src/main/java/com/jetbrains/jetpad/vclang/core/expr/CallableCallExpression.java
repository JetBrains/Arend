package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.Callable;

import java.util.List;

public interface CallableCallExpression {
  Callable getDefinition();
  List<? extends Expression> getDefCallArguments();
}
