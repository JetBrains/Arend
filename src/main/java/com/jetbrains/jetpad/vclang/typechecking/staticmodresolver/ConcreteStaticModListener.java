package com.jetbrains.jetpad.vclang.typechecking.staticmodresolver;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class ConcreteStaticModListener implements StaticModListener {
  @Override
  public void resolveStaticMod(Abstract.DefineStatement stat, boolean isStatic) {
      ((Concrete.DefineStatement)stat).setExplicitStaticMod(isStatic);
  }
}
