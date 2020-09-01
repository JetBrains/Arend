package org.arend.typechecking.computation;

import org.arend.naming.reference.TCDefReferable;
import org.arend.util.ComputationInterruptedException;

public interface CancellationIndicator {
  boolean isCanceled();
  void cancel();

  default void cancel(TCDefReferable target) {

  }

  default void checkCanceled() throws ComputationInterruptedException {
    if (isCanceled()) {
      throw new ComputationInterruptedException();
    }
  }
}
