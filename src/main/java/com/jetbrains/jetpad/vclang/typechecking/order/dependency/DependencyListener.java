package com.jetbrains.jetpad.vclang.typechecking.order.dependency;

import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;

public interface DependencyListener {
  void dependsOn(TCReferable def1, boolean header, TCReferable def2);
}
