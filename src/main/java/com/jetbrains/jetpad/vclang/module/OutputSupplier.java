package com.jetbrains.jetpad.vclang.module;

public interface OutputSupplier {
  Output getOutput(Module module);
  Output locateOutput(Module module);
}
