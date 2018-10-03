package org.arend.typechecking.order.dependency;

import org.arend.naming.reference.TCReferable;

import java.util.Collections;
import java.util.Set;

public class DummyDependencyListener implements DependencyListener {
  public static final DummyDependencyListener INSTANCE = new DummyDependencyListener();

  private DummyDependencyListener() { }

  @Override
  public void dependsOn(TCReferable def1, boolean header, TCReferable def2) {

  }

  @Override
  public Set<? extends TCReferable> update(TCReferable definition) {
    return Collections.emptySet();
  }
}
