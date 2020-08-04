package org.arend.typechecking.computation;

import org.arend.naming.reference.TCReferable;

public class DefinitionCancellationIndicator implements CancellationIndicator {
  private boolean myCancelled = false;
  private final TCReferable myTarget;

  public DefinitionCancellationIndicator(TCReferable target) {
    myTarget = target;
  }

  @Override
  public boolean isCanceled() {
    return myCancelled;
  }

  @Override
  public void cancel() {
    myCancelled = true;
  }

  @Override
  public void cancel(TCReferable target) {
    if (myTarget == target) {
      myCancelled = true;
    }
  }
}
