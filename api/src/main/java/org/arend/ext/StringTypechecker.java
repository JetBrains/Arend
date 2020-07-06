package org.arend.ext;

import org.arend.ext.typechecking.ContextData;
import org.arend.ext.typechecking.ExpressionTypechecker;
import org.arend.ext.typechecking.TypedExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public interface StringTypechecker {
  @Nullable TypedExpression typecheckString(@NotNull String unescapedString, @NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData);
}
