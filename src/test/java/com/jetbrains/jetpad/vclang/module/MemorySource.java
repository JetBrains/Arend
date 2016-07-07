package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.source.ParseSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class MemorySource extends ParseSource {
  private final MemorySourceSupplier.MemorySourceEntry myEntry;

  public MemorySource(ErrorReporter errorReporter, PathModuleID module, MemorySourceSupplier.MemorySourceEntry entry) {
    super(errorReporter, module);
    myEntry = entry;
    setStream(entry.source == null ? null : new ByteArrayInputStream(entry.source.getBytes()));
  }

  @Override
  public long lastModified() {
    return myEntry.lastModified;
  }

  @Override
  public ModuleLoader.Result load() throws IOException {
    return super.load();
  }
}
