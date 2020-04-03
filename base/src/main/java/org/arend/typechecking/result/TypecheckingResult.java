package org.arend.typechecking.result;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.AppExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.PiExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.typechecking.TypedExpression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.jetbrains.annotations.NotNull;

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
    PiExpression pi = type.cast(PiExpression.class);
    return pi != null ? pi.getParameters() : EmptyDependentLink.getInstance();
  }

  @Override
  public TypecheckingResult applyExpression(Expression expr, boolean isExplicit, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    expression = AppExpression.make(expression, expr, isExplicit);
    Expression newType = type.applyExpression(expr);
    if (newType == null) {
      errorReporter.report(new TypecheckingError("Expected an expression of a pi type", sourceNode));
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
  public CoreExpression getType() {
    return type;
  }
}
