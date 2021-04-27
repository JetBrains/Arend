package org.arend.core.context.param;

import org.arend.core.expr.DataCallExpression;
import org.arend.core.subst.LevelPair;
import org.arend.prelude.Prelude;

import java.util.Collections;

public class UnusedIntervalDependentLink extends TypedSingleDependentLink {
  public static final UnusedIntervalDependentLink INSTANCE = new UnusedIntervalDependentLink();

  private UnusedIntervalDependentLink() {
    super(true, null, new DataCallExpression(Prelude.INTERVAL, LevelPair.PROP, Collections.emptyList()));
  }
}
