package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;

import java.util.*;

public class MemorySourceSupplier implements SourceSupplier {
  static class MemorySourceEntry {
    final String source;
    final long lastModified;

    private MemorySourceEntry(String source) {
      this.source = source;
      this.lastModified = System.nanoTime();
    }
  }

  private final ErrorReporter myErrorReporter;
  private final Map<ModulePath, MemorySourceEntry> myMap = new HashMap<>();

  public MemorySourceSupplier(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  public void add(ModulePath modulePath, String source) {
    myMap.put(modulePath, new MemorySourceEntry(source));
  }

  public void remove(ModulePath modulePath) {
    myMap.remove(modulePath);
  }

  public void touch(ModulePath modulePath) {
    add(modulePath, myMap.get(modulePath).source);
  }

  @Override
  public Source getSource(ModuleID module) {
    if (!(module instanceof PathModuleID)) {
      return null;
    }
    if (!myMap.containsKey(module.getModulePath()))
      return null;

    return new MemorySource(myErrorReporter, (PathModuleID) module, myMap.get(module.getModulePath()));
  }

  @Override
  public PathModuleID locateModule(ModulePath module) {
    return myMap.containsKey(module) ? new PathModuleID(module) : null;
  }
}
