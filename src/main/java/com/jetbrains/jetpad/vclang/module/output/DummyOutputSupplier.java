package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.Namespace;

public class DummyOutputSupplier implements OutputSupplier {
  private final DummyOutput myOutput = new DummyOutput();

  private DummyOutputSupplier() {}

  private static DummyOutputSupplier INSTANCE = new DummyOutputSupplier();

  public static DummyOutputSupplier getInstance() {
    return INSTANCE;
  }

  @Override
  public DummyOutput getOutput(Namespace module) {
    return myOutput;
  }

  @Override
  public DummyOutput locateOutput(Namespace module) {
    return myOutput;
  }
}
