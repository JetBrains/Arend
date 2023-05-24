package org.arend.ext.reference;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.jetbrains.annotations.NotNull;

public record UnparsedSequenceElem(@NotNull ConcreteExpression expression, @NotNull Fixity fixity, boolean isExplicit) {
}
