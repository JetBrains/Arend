package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class WeakDataLocalReferable extends LocalReferable implements DataContainer {
  private final WeakReference<Object> myData;

  public WeakDataLocalReferable(Object data, String name) {
    super(name);
    myData = new WeakReference<>(data);
  }

  @Nullable
  @Override
  public Object getData() {
    return myData.get();
  }
}
