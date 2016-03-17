package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

public interface TypecheckedReporter {
  void typecheckingSucceeded(Abstract.Definition abstractDefinition, Definition definition);
  void typecheckingFailed(Abstract.Definition definition);
}
