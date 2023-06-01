package org.arend.typechecking.result;

import org.arend.core.context.binding.TypedEvaluatingBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.AppExpression;
import org.arend.core.expr.DataExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.PiExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.ext.core.context.CoreEvaluatingBinding;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.typechecking.TypedExpression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TypecheckingResult implements TResult, TypedExpression {
  public Expression expression;
  public Expression type;

  public TypecheckingResult(Expression expression, Expression type) {
    this.expression = expression;
    this.type = type;
  }

  public static TypecheckingResult fromChecked(TypedExpression expression) {
    if (!(expression == null || expression instanceof TypecheckingResult)) {
      throw new IllegalStateException("CheckedExpression must be TypecheckingResult");
    }
    return (TypecheckingResult) expression;
  }

  @Override
  public TypecheckingResult toResult(CheckTypeVisitor typechecker) {
    return this;
  }

  @Override
  public DependentLink getParameter() {
    type = type.normalize(NormalizationMode.WHNF);
    return type instanceof PiExpression ? ((PiExpression) type).getParameters() : EmptyDependentLink.getInstance();
  }

  @Override
  public TypecheckingResult applyExpression(Expression expr, boolean isExplicit, CheckTypeVisitor typechecker, Concrete.SourceNode sourceNode) {
    expression = AppExpression.make(expression, expr, isExplicit);
    Expression newType = type.applyExpression(expr);
    if (newType == null) {
      typechecker.getErrorReporter().report(new TypecheckingError("Expected an expression of a pi type", sourceNode));
    } else {
      type = newType;
    }
    return this;
  }

  @Override
  public List<SingleDependentLink> getImplicitParameters() {
    List<SingleDependentLink> params = new ArrayList<>();
    type.getPiParameters(params, true);
    return params;
  }

  @NotNull
  @Override
  public CoreExpression getExpression() {
    return expression;
  }

  @NotNull
  @Override
  public Expression getType() {
    return type;
  }

  @Override
  public @NotNull CoreEvaluatingBinding makeEvaluatingBinding(@Nullable String name) {
    return new TypedEvaluatingBinding(name, expression, type);
  }

  @Override
  public @Nullable TypecheckingResult replaceType(@NotNull CoreExpression type) {
    if (!(type instanceof Expression)) {
      throw new IllegalArgumentException();
    }
    return this.type.isError() ? this : CompareVisitor.compare(DummyEquations.getInstance(), CMP.LE, this.type, (Expression) type, Type.OMEGA, null) ? new TypecheckingResult(expression, (Expression) type) : null;
  }

  @Override
  public @NotNull TypecheckingResult normalizeType() {
    return new TypecheckingResult(expression, type.normalize(NormalizationMode.WHNF));
  }

  @Override
  public @NotNull TypedExpression makeDataExpression(@Nullable Object metaData) {
    return new TypecheckingResult(new DataExpression(expression, metaData), type);
  }
}
