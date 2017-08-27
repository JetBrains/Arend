package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.TypecheckingUnit;

public interface DependencyListener<T> {
  default void sccFound(SCC<T> scc) {}
  default void unitFound(TypecheckingUnit<T> unit, Recursion recursion) {}
  default boolean needsOrdering(Concrete.Definition<T> definition) { return true; }

  default void alreadyTypechecked(Concrete.Definition<T> definition) {}
  default void dependsOn(Typecheckable<T> unit, Concrete.Definition<T> def) {}

  enum Recursion { NO, IN_HEADER, IN_BODY }
}
