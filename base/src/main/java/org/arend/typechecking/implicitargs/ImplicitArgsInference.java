package org.arend.typechecking.implicitargs;

import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.result.TResult;

public interface ImplicitArgsInference {
  TResult infer(Concrete.AppExpression expr, Expression expectedType);
  TResult inferTail(TResult fun, Expression expectedType, Concrete.Expression expr);
  InferenceVariable newInferenceVariable(Expression expectedType, Concrete.SourceNode sourceNode);
}
