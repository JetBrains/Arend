package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.TypecheckingUnit;

public interface DependencyListener {
  default void sccFound(SCC scc) {}
  default void unitFound(TypecheckingUnit unit, Recursion recursion) {}
  default boolean needsOrdering(GlobalReferable definition) { return false; }

  default void alreadyTypechecked(GlobalReferable definition) {}
  default void dependsOn(Typecheckable unit, GlobalReferable def) {}

  enum Recursion { NO, IN_HEADER, IN_BODY }
}
