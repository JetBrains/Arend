package org.arend.typechecking.order.dependency;

import org.arend.naming.reference.TCReferable;

import java.util.Set;

public interface DependencyListener {
  void dependsOn(TCReferable def1, boolean header, TCReferable def2);
  Set<? extends TCReferable> update(TCReferable definition);
}
