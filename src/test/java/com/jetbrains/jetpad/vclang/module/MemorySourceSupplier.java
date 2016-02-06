package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

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

  private final ModuleLoader myModuleLoader;
  private final ErrorReporter myErrorReporter;
  private final Map<ModulePath, MemorySourceEntry> myMap = new HashMap<>();

  public MemorySourceSupplier(ModuleLoader moduleLoader, ErrorReporter errorReporter) {
    myModuleLoader = moduleLoader;
    myErrorReporter = errorReporter;
  }

  public void add(ModulePath modulePath, String source) {
    myMap.put(modulePath, new MemorySourceEntry(source));
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

    return new MemorySource(myModuleLoader, myErrorReporter, (PathModuleID) module, myMap.get(module.getModulePath()));
  }

  @Override
  public PathModuleID locateModule(ModulePath module) {
    return myMap.containsKey(module) ? new PathModuleID(module) : null;
  }
}
