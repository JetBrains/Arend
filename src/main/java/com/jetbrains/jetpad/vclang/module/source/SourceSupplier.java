package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.Namespace;

public interface SourceSupplier {
  Source getSource(Namespace module);
}
