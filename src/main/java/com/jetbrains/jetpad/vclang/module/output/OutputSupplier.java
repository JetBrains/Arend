package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.Namespace;

public interface OutputSupplier {
  Output getOutput(Namespace module);
  Output locateOutput(Namespace module);
}
