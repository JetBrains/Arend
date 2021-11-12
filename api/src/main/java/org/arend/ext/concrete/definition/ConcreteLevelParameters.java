package org.arend.ext.concrete.definition;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ConcreteLevelParameters extends ConcreteSourceNode {
  @NotNull List<? extends ArendRef> getReferables();
  boolean isIncreasing();
}
