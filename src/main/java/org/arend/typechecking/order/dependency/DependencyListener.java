package org.arend.typechecking.order.dependency;

import org.arend.naming.reference.TCReferable;

public interface DependencyListener {
  void dependsOn(TCReferable def1, boolean header, TCReferable def2);
}
