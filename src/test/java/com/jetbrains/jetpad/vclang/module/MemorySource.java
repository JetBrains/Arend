package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.ParseSource;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.io.ByteArrayInputStream;

public class MemorySource extends ParseSource {
  private long myLastModified;

  public MemorySource(ModuleLoader moduleLoader, ErrorReporter errorReporter, ResolvedName module, String source) {
    super(moduleLoader, errorReporter, module);
    if (source != null) {
      setStream(new ByteArrayInputStream(source.getBytes()));
    }
    myLastModified = System.nanoTime();
  }

  @Override
  public boolean isAvailable() {
    return getStream() != null;
  }

  @Override
  public long lastModified() {
    return myLastModified;
  }

  public void touch() {
    myLastModified = System.nanoTime();
  }

  @Override
  public boolean isContainer() {
    return false;
  }
}
