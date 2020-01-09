package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreEvaluatingBinding;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface CoreLetExpression extends CoreExpression {
  @Nonnull CoreExpression getExpression();
  @Nonnull Collection<? extends CoreEvaluatingBinding> getClauses();
}
