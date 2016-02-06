package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.ParseSource;
import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class MemorySource extends ParseSource {
  private final MemorySourceSupplier.MemorySourceEntry myEntry;

  public MemorySource(ModuleLoader moduleLoader, ErrorReporter errorReporter, PathModuleID module, MemorySourceSupplier.MemorySourceEntry entry) {
    super(moduleLoader, errorReporter, module);
    myEntry = entry;
    setStream(entry.source == null ? null : new ByteArrayInputStream(entry.source.getBytes()));
  }

  @Override
  public boolean isAvailable() {
    return true;
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
