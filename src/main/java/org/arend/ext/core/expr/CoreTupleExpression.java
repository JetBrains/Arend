package org.arend.ext.core.expr;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface CoreTupleExpression extends CoreExpression {
  @Nonnull Collection<? extends CoreExpression> getFields();
  @Nonnull CoreSigmaExpression getSigmaType();
}
