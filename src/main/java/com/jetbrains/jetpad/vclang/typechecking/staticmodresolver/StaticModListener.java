package com.jetbrains.jetpad.vclang.typechecking.staticmodresolver;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface StaticModListener {
  void resolveStaticMod(Abstract.DefineStatement stat, boolean isStatic);
}
