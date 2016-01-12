package com.jetbrains.jetpad.vclang.term.expr.param;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.Map;

public interface DependentLink extends Binding {
  boolean isExplicit();
  DependentLink getNext();
  DependentLink copy(Map<Binding, Expression> substs);
}
