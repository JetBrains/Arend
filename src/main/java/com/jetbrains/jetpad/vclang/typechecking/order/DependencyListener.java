package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;

public interface DependencyListener {
  void sccFound(SCC scc);
  void alreadyTypechecked(Definition definition);
  void dependsOn(Typecheckable unit, Abstract.Definition def);
}
