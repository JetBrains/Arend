package org.arend.ext.core.expr;

import org.arend.ext.core.elimtree.CoreBody;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CoreExpression extends CoreBody {
  @Nonnull CoreExpression getUnderlyingExpression();
  @Nullable CoreExpression getType();
}
