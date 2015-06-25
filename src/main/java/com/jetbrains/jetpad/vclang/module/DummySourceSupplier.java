package com.jetbrains.jetpad.vclang.module;

public class DummySourceSupplier implements SourceSupplier {
  private final DummySource mySource = new DummySource();

  private DummySourceSupplier() {}

  private static DummySourceSupplier INSTANCE = new DummySourceSupplier();

  public static DummySourceSupplier getInstance() {
    return INSTANCE;
  }

  @Override
  public DummySource getSource(Module module) {
    return mySource;
  }
}
