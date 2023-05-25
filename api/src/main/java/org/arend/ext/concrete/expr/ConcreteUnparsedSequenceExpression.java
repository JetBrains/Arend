package org.arend.ext.concrete.expr;

import org.arend.ext.reference.ConcreteUnparsedSequenceElem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ConcreteUnparsedSequenceExpression extends ConcreteExpression {
  @NotNull List<? extends ConcreteUnparsedSequenceElem> getSequence();
  @Nullable ConcreteClauses getClauses();
}
