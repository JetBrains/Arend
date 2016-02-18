package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.ParseSource;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class MemorySource extends ParseSource {
  private final MemorySourceSupplier.MemorySourceEntry myEntry;

  public MemorySource(ModuleLoader moduleLoader, ErrorReporter errorReporter, ResolvedName module, MemorySourceSupplier.MemorySourceEntry entry) {
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
  public boolean isContainer() {
    return getStream() == null;
  }

  @Override
  public ModuleLoadingResult load(boolean childrenOnly) throws IOException {
    Namespace namespace = getModule().parent.getChild(getModule().name.name);
    for (String childName : myEntry.children) {
      namespace.getChild(childName);
    }
    if (!childrenOnly && getStream() != null) {
      return super.load(false);
    } else {
      return new ModuleLoadingResult(getModule().toNamespaceMember(), false, 0);
    }
  }
}
