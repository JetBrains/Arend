package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.TypecheckingUnit;

public interface DependencyListener {
  default void sccFound(SCC scc) {}
  default void unitFound(TypecheckingUnit unit, Recursion recursion) {}
  default boolean needsOrdering(Concrete.Definition definition) { return false; }

  default void alreadyTypechecked(Concrete.Definition definition) {}
  default void dependsOn(Typecheckable unit, Concrete.Definition def) {}

  enum Recursion { NO, IN_HEADER, IN_BODY }
}
