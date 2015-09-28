package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Prelude;

public class RootModule {
  public static Namespace ROOT = new Namespace("\\root");

  public static void initialize() {
    Prelude.PRELUDE.setParent(ROOT);
    ROOT.clear();
    ROOT.addChild(Prelude.PRELUDE);
  }
}
