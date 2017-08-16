package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface TypecheckableProvider {
  Abstract.Definition forReferable(Abstract.GlobalReferableSourceNode referable);
}
