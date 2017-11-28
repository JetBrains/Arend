package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.Definition;

public interface TypecheckedReporter {
  void typecheckingFinished(Definition definition);

  TypecheckedReporter DUMMY = definition -> {};
}
