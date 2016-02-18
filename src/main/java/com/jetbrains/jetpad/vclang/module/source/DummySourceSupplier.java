package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModulePath;

public class DummySourceSupplier implements SourceSupplier {
  private DummySourceSupplier() {}

  private static DummySourceSupplier INSTANCE = new DummySourceSupplier();

  public static SourceSupplier getInstance() {
    return  INSTANCE;
  }

  @Override
  public Source getSource(ModuleID module) {
    return null;
  }

  @Override
  public ModuleID locateModule(ModulePath modulePath) {
    return null;
  }
}
