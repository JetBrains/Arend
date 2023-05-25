package org.arend.ext.concrete;

import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.reference.ConcreteUnparsedSequenceElem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ResolvedApplication(@NotNull ConcreteExpression function,
                                  @Nullable List<? extends ConcreteUnparsedSequenceElem> leftElements,
                                  @Nullable List<? extends ConcreteUnparsedSequenceElem> rightElements,
                                  @Nullable List<ConcreteArgument> arguments) {
}
