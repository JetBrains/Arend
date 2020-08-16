package org.arend.ext.concrete.expr;

import org.arend.ext.concrete.ConcreteClause;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ConcreteClauses extends ConcreteSourceNode {
  @NotNull List<? extends ConcreteClause> getClauseList();
}
