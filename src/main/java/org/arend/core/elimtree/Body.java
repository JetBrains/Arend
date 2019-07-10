package org.arend.core.elimtree;

import org.arend.core.expr.Expression;
import org.arend.util.Decision;

import java.util.List;

public interface Body {
  Decision isWHNF(List<? extends Expression> arguments, boolean normalizing);
  Expression getStuckExpression(List<? extends Expression> arguments, Expression expression, boolean normalizing);
}
