package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

public interface TypecheckedReporter {
  void typecheckingSucceeded(ResolvedName definitionName);
  void typecheckingFailed(ResolvedName definitionName);
}
