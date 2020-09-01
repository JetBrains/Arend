package org.arend.typechecking.computation;

import org.arend.naming.reference.TCDefReferable;

public class DefinitionCancellationIndicator implements CancellationIndicator {
  private boolean myCancelled = false;
  private final TCDefReferable myTarget;

  public DefinitionCancellationIndicator(TCDefReferable target) {
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
  public void cancel(TCDefReferable target) {
    if (myTarget == target) {
      myCancelled = true;
    }
  }
}
