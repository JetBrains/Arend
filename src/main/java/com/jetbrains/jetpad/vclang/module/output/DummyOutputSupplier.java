package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModulePath;

public class DummyOutputSupplier implements OutputSupplier {
  private DummyOutputSupplier() {}

  private static DummyOutputSupplier INSTANCE = new DummyOutputSupplier();

  public static OutputSupplier getInstance() {
    return INSTANCE;
  }

  @Override
  public Output getOutput(ModuleID module) {
    return null;
  }

  @Override
  public ModuleID locateModule(ModulePath modulePath) {
    return null;
  }
}
