package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

public interface SourceSupplier {
  Source getSource(ResolvedName module);
}
