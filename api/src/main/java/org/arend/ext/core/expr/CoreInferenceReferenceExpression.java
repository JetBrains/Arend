package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreInferenceVariable;

public interface CoreInferenceReferenceExpression extends CoreExpression {
  CoreInferenceVariable getVariable();
  CoreExpression getSubstExpression();
}
