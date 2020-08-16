package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ConcreteCoclauses extends ConcreteSourceNode {
  @NotNull List<? extends ConcreteCoclause> getCoclauseList();
}
