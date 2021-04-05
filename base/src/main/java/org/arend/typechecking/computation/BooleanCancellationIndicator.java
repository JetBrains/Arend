package org.arend.typechecking.computation;

public class BooleanCancellationIndicator implements CancellationIndicator {
    public boolean isCancelled = false;

    @Override
    public boolean isCanceled() {
        return isCancelled;
    }

    @Override
    public void cancel() {
        isCancelled = true;
    }
}
