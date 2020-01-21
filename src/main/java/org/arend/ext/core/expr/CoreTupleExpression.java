package org.arend.ext.core.expr;

import javax.annotation.Nonnull;
import java.util.List;

public interface CoreTupleExpression extends CoreExpression {
  @Nonnull List<? extends CoreExpression> getFields();
  @Nonnull CoreSigmaExpression getSigmaType();
}
