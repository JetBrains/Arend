package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckingUnit;

public interface DependencyListener {
  void sccFound(SCC scc);
  void unitFound(TypecheckingUnit unit, Recursion recursion);
  boolean needsOrdering(Abstract.Definition definition);

  void alreadyTypechecked(Abstract.Definition definition);
  void dependsOn(Typecheckable unit, Abstract.Definition def);

  enum Recursion { NO, IN_HEADER, IN_BODY }
}
