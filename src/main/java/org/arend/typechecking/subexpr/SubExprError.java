package org.arend.typechecking.subexpr;

public enum SubExprError {
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
  ConcreteCoreMismatch,
  Other
}
