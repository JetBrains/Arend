package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.elimtree.CoreElimTree;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public interface CoreCaseExpression {
  boolean isSCase();
  @Nonnull CoreParameter getParameters();
  @Nonnull CoreExpression getResultType();
  @Nullable CoreExpression getResultTypeLevel();
  @Nonnull CoreElimTree getElimTree();
  @Nonnull Collection<? extends CoreExpression> getArguments();
}
