package org.arend.ext.reference;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteClauses;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ResolvedApplication(@NotNull ConcreteExpression function,
                                  @Nullable List<UnparsedSequenceElem> leftElements,
                                  @Nullable List<UnparsedSequenceElem> rightElements,
                                  @Nullable List<ConcreteArgument> arguments,
                                  @Nullable ConcreteClauses clauses) {
}
