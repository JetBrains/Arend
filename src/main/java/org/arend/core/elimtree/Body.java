package org.arend.core.elimtree;

import org.arend.core.expr.Expression;

import java.util.List;

public interface Body {
  boolean isWHNF(List<? extends Expression> arguments);
  Expression getStuckExpression(List<? extends Expression> arguments, Expression expression);
}
