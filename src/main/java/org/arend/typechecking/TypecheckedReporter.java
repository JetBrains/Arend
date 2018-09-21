package org.arend.typechecking;

import org.arend.core.definition.Definition;

public interface TypecheckedReporter {
  void typecheckingFinished(Definition definition);

  TypecheckedReporter DUMMY = definition -> {};
}
