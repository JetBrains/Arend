package org.arend.typechecking.subexpr;

import org.arend.core.expr.Expression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SubExprError {
  private final @NotNull Kind kind;
  private @Nullable Expression errorExpr;

  public SubExprError(@NotNull Kind kind, @Nullable Expression errorExpr) {
    this.kind = kind;
    this.errorExpr = errorExpr;
  }

  public static SubExprError mismatch(@NotNull Expression errorExpr) {
    return new SubExprError(Kind.ConcreteCoreMismatch, errorExpr);
  }

  public SubExprError(@NotNull Kind kind) {
    this(kind, null);
  }

  public void setErrorExpr(@NotNull Expression errorExpr) {
    this.errorExpr = errorExpr;
  }

  public @Nullable Expression getErrorExpr() {
    return errorExpr;
  }

  public @NotNull Kind getKind() {
    return kind;
  }

  public enum Kind {
    ExpectPi,
    /**
     * In an expression with binding, there isn't a type in the binding.
     */
    ConcreteHasNoTypeBinding,
    /**
     * When matching a list of expressions, the list doesn't match.
     */
    ExprListNoMatch,
    /**
     * Concrete expression and core expression doesn't have the corresponded type,
     * like when you have a {@link org.arend.term.concrete.Concrete.ProjExpression}
     * but the core expression is not a {@link org.arend.core.expr.ProjExpression}.
     */
    ConcreteCoreMismatch
  }
}

