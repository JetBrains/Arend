package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

public class DummyOutputSupplier implements OutputSupplier {
  private final DummyOutput myOutput = new DummyOutput();

  private DummyOutputSupplier() {}

  private static DummyOutputSupplier INSTANCE = new DummyOutputSupplier();

  public static DummyOutputSupplier getInstance() {
    return INSTANCE;
  }

  @Override
  public DummyOutput getOutput(ResolvedName module) {
    return myOutput;
  }

  @Override
  public DummyOutput locateOutput(ResolvedName module) {
    return myOutput;
  }
}
