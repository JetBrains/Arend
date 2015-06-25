package com.jetbrains.jetpad.vclang.module;

import java.io.ByteArrayInputStream;

public class MemorySource extends ParseSource {
  public MemorySource(ModuleLoader moduleLoader, Module module, String source) {
    super(moduleLoader, module);
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
