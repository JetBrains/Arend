package org.arend.typechecking.order.dependency;

import org.arend.naming.reference.TCReferable;

public class DummyDependencyListener implements DependencyListener {
  public static final DummyDependencyListener INSTANCE = new DummyDependencyListener();

  private DummyDependencyListener() { }

  @Override
  public void dependsOn(TCReferable def1, boolean header, TCReferable def2) {

  }
}
