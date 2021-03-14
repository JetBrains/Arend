package org.arend.ext;

import org.arend.ext.concrete.pattern.ConcreteNumberPattern;
import org.arend.ext.concrete.pattern.ConcretePattern;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.typechecking.ContextData;
import org.arend.ext.typechecking.ExpressionTypechecker;
import org.arend.ext.typechecking.PatternContextData;
import org.arend.ext.typechecking.TypedExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public interface LiteralTypechecker {
  /**
   * @param unescapedString the string literal, unescaped.
   * @return <code>null</code> if check failed
   */
  @Nullable TypedExpression typecheckString(@NotNull String unescapedString, @NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData);

  /**
   *
   * @param number the integer literal
   * @return <code>null</code> if check failed
   * @see ExpressionTypechecker#checkNumber(BigInteger, CoreExpression, ConcreteExpression)
   */
  default @Nullable TypedExpression typecheckNumber(@NotNull BigInteger number, @NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    return typechecker.checkNumber(number, contextData.getExpectedType(), contextData.getMarker());
  }

  /**
   * @param pattern the number pattern
   * @return null if check failed
   * Do <strong>not</strong> return a number pattern.
   */
  default @Nullable ConcretePattern desugarNumberPattern(@NotNull ConcreteNumberPattern pattern, @NotNull ExpressionTypechecker typechecker, @NotNull PatternContextData contextData) {
    return typechecker.desugarNumberPattern(pattern);
  }
}
