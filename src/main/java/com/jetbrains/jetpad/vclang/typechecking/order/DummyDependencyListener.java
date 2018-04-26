package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;

public class DummyDependencyListener implements DependencyListener {
  public static final DummyDependencyListener INSTANCE = new DummyDependencyListener();

  private DummyDependencyListener() { }

  @Override
  public void dependsOn(TCReferable def1, boolean header, TCReferable def2) {

  }
}
