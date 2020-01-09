package org.arend.core.elimtree;

import org.arend.core.expr.Expression;
import org.arend.ext.core.elimtree.CoreBody;
import org.arend.util.Decision;

import java.util.List;

public interface Body extends CoreBody {
  Decision isWHNF(List<? extends Expression> arguments);
  Expression getStuckExpression(List<? extends Expression> arguments, Expression expression);
}
