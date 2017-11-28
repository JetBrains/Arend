package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

public interface DependencyListener {
  void dependsOn(GlobalReferable def1, boolean header, GlobalReferable def2);
}
