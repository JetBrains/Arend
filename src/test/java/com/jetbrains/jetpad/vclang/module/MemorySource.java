package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.ParseSource;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;

import java.io.ByteArrayInputStream;

public class MemorySource extends ParseSource {
  public MemorySource(ModuleLoader moduleLoader, ErrorReporter errorReporter, Namespace module, String source) {
    super(moduleLoader, errorReporter, module);
    if (source != null) {
      setStream(new ByteArrayInputStream(source.getBytes()));
    }
  }

  @Override
  public boolean isAvailable() {
    return getStream() != null;
  }

  @Override
  public long lastModified() {
    return 0;
  }
}
