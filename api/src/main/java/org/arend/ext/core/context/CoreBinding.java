package org.arend.ext.core.context;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.expr.CoreReferenceExpression;
import org.arend.ext.variable.Variable;

/**
 * Represents a local binding.
 */
public interface CoreBinding extends Variable {
  CoreExpression getTypeExpr();
  CoreReferenceExpression makeReference();
}
