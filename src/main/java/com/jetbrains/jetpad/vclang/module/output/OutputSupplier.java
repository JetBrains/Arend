package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

public interface OutputSupplier {
  Output getOutput(ResolvedName module);
  Output locateOutput(ResolvedName module);
}
