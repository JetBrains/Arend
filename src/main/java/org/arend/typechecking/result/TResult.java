package org.arend.typechecking.result;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.error.ErrorReporter;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.List;

public interface TResult {
  TypecheckingResult toResult(CheckTypeVisitor typechecker);
  DependentLink getParameter();
  TResult applyExpression(Expression expression, ErrorReporter errorReporter, Concrete.SourceNode sourceNode);
  List<? extends DependentLink> getImplicitParameters();
}