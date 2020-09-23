package org.arend.typechecking.computation;

import java.util.function.Supplier;

public class BooleanComputationRunner extends ComputationRunner<Boolean> {
  @Override
  public Boolean run(CancellationIndicator cancellationIndicator, Supplier<Boolean> runnable) {
    Boolean result = super.run(cancellationIndicator, runnable);
    return result != null && result;
  }
}
