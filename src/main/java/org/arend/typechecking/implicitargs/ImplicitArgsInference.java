package org.arend.typechecking.implicitargs;

import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.expr.Expression;
import org.arend.core.expr.type.ExpectedType;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.result.TResult;

public interface ImplicitArgsInference {
  TResult infer(Concrete.AppExpression expr, ExpectedType expectedType);
  TResult inferTail(TResult fun, ExpectedType expectedType, Concrete.Expression expr);
  InferenceVariable newInferenceVariable(Expression expectedType, Concrete.SourceNode sourceNode);
}
