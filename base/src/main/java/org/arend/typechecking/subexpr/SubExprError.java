package org.arend.typechecking.subexpr;

import org.arend.core.expr.Expression;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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

  @Override
  public @NotNull @Nls String toString() {
    switch (getKind()) {
      case MetaRef:
        return "trying to match a tactic: " + errorExpr;
      case FieldNotFound:
        return "a corresponding field is missing";
      case MissingImplementationField:
        return "the implementation field seems missing";
      case MissingExpr:
        return "an expression is missing, like in a clause";
      case ExpectLam:
        return "expect the lambda body " + errorExpr +
            " to be another lambda expression";
      case Clauses:
        return "when matching the clauses of a let expression, the list doesn't match";
      case Telescope:
        return "when matching the parameter types of " +
            Optional.ofNullable(errorExpr).map(Expression::toString).orElse("a telescopic expression") +
            ", the list doesn't match";
      case Arguments:
        return "when matching the arguments of an application " + errorExpr +
            ", the list doesn't match";
      case ExpectPi:
        return "expect the type " + errorExpr + " to be a pi type";
      case ConcreteHasNoTypeBinding:
        return "there isn't a type annotation in the binding of type " + errorExpr;
      case ExprListNoMatch:
        return "when matching a list of expressions, the list doesn't match";
      case ConcreteCoreMismatch:
        return "the core expression doesn't have the corresponded type of the concrete one";
    }
    return "unknown error happened";
  }

  /**
   * @see SubExprError#toString()
   */
  public enum Kind {
    /**
     * Trying to match a function whose argument is a
     * {@link org.arend.naming.reference.MetaReferable}
     */
    MetaRef,
    FieldNotFound,
    MissingImplementationField,
    MissingExpr,
    ExpectLam,
    Clauses,
    Telescope,
    Arguments,
    ExpectPi,
    ConcreteHasNoTypeBinding,
    ExprListNoMatch,
    /**
     * Concrete expression and core expression doesn't have the corresponded type,
     * like when you have a {@link org.arend.term.concrete.Concrete.ProjExpression}
     * but the core expression is not a {@link org.arend.core.expr.ProjExpression}.
     */
    ConcreteCoreMismatch
  }
}

