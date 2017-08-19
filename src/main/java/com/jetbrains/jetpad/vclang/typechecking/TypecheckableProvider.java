package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;

public interface TypecheckableProvider<T> {
  Concrete.Definition<T> forReferable(Abstract.GlobalReferableSourceNode referable);
}
