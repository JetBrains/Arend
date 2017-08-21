package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

public interface TypecheckableProvider<T> {
  Concrete.Definition<T> forReferable(GlobalReferable referable);
}
