package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.ParseSource;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class MemorySource extends ParseSource {
  private long myLastModified;
  private final List<String> myChildren;

  public MemorySource(ModuleLoader moduleLoader, ErrorReporter errorReporter, ResolvedName module, String source) {
    super(moduleLoader, errorReporter, module);
    if (source != null) {
      setStream(new ByteArrayInputStream(source.getBytes()));
    }
    myLastModified = System.nanoTime();
    myChildren = new ArrayList<>();
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

  void addChild(String name) {
    myChildren.add(name);
  }
}
