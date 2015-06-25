package com.jetbrains.jetpad.vclang.module;

public class DummyOutputSupplier implements OutputSupplier {
  private final DummyOutput myOutput = new DummyOutput();

  private DummyOutputSupplier() {}

  private static DummyOutputSupplier INSTANCE = new DummyOutputSupplier();

  public static DummyOutputSupplier getInstance() {
    return INSTANCE;
  }

  @Override
  public DummyOutput getOutput(Module module) {
    return myOutput;
  }

  @Override
  public DummyOutput locateOutput(Module module) {
    return myOutput;
  }
}
