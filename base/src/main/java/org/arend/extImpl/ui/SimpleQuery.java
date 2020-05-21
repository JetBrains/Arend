package org.arend.extImpl.ui;

import org.arend.ext.ui.ArendQuery;
import org.jetbrains.annotations.Nullable;

public class SimpleQuery<T> implements ArendQuery<T> {
  private T myResult;

  @Nullable
  @Override
  public T getResult() {
    return myResult;
  }

  public void setResult(T result) {
    myResult = result;
  }
}
