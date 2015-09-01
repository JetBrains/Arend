package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;

import java.util.HashMap;
import java.util.Map;

public class MemorySourceSupplier implements SourceSupplier {
  private final ModuleLoader myModuleLoader;
  private final ErrorReporter myErrorReporter;
  private final Map<Namespace, String> myMap = new HashMap<>();

  public MemorySourceSupplier(ModuleLoader moduleLoader, ErrorReporter errorReporter) {
    myModuleLoader = moduleLoader;
    myErrorReporter = errorReporter;
  }

  public void add(Namespace module, String source) {
    myMap.put(module, source);
  }

  @Override
  public MemorySource getSource(Namespace module) {
    return new MemorySource(myModuleLoader, myErrorReporter, module, myMap.get(module));
  }
}
