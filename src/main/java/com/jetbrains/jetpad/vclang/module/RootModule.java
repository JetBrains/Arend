package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

public class RootModule {
  public static Namespace ROOT = new Namespace(new Utils.Name("\\root"), null);

  public static void initialize() {
    Prelude.PRELUDE.setParent(ROOT);
    ROOT.clear();
    ROOT.addChild(Prelude.PRELUDE);
  }
}
