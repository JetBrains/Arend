package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckingUnit;

public interface DependencyListener {
  void sccFound(SCC scc);
  void unitFound(TypecheckingUnit unit, Recursion recursion);

  void alreadyTypechecked(Definition definition);
  void dependsOn(Typecheckable unit, Abstract.Definition def);

  enum Recursion { NO, IN_HEADER, IN_BODY }
}
