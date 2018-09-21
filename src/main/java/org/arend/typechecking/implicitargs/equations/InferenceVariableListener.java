package org.arend.typechecking.implicitargs.equations;

import org.arend.core.expr.InferenceReferenceExpression;

public interface InferenceVariableListener {
  void solved(Equations equations, InferenceReferenceExpression referenceExpression);
}
