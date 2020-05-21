package org.arend.extImpl.ui;

import org.arend.ext.ui.ArendQuery;
import org.jetbrains.annotations.Nullable;

public class DelegateQuery<T> implements ArendQuery<T> {
  private ArendQuery<T> myDelegate;

  public void setDelegate(ArendQuery<T> delegate) {
    myDelegate = delegate;
  }

  @Nullable
  @Override
  public T getResult() {
    return myDelegate == null ? null : myDelegate.getResult();
  }
}
