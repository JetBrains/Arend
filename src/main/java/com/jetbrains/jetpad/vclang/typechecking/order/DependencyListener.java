package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.Typecheckable;

public interface DependencyListener {
  default void dependsOn(Typecheckable unit, GlobalReferable def) {}
}
