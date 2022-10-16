package org.arend.core.context.param;

import org.arend.core.expr.DataCallExpression;
import org.arend.core.subst.Levels;
import org.arend.prelude.Prelude;

import java.util.Collections;

public class UnusedIntervalDependentLink extends TypedSingleDependentLink {
  public static final UnusedIntervalDependentLink INSTANCE = new UnusedIntervalDependentLink();

  private UnusedIntervalDependentLink() {
    super(true, null, DataCallExpression.make(Prelude.INTERVAL, Levels.EMPTY, Collections.emptyList()));
  }
}
