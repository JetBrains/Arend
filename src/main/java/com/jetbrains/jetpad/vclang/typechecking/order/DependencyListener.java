package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckingUnit;

public interface DependencyListener {
  default void sccFound(SCC scc) {}
  default void unitFound(TypecheckingUnit unit, Recursion recursion) {}
  default boolean needsOrdering(Abstract.Definition definition) { return true; }

  default void alreadyTypechecked(Abstract.Definition definition) {}
  default void dependsOn(Typecheckable unit, Abstract.Definition def) {}

  enum Recursion { NO, IN_HEADER, IN_BODY }
}
