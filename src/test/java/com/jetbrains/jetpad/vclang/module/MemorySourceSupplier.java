package com.jetbrains.jetpad.vclang.module;

import java.util.HashMap;
import java.util.Map;

public class MemorySourceSupplier implements SourceSupplier {
  private final ModuleLoader myModuleLoader;
  private final Map<Module, String> myMap = new HashMap<>();

  public MemorySourceSupplier(ModuleLoader moduleLoader) {
    myModuleLoader = moduleLoader;
  }

  public void add(Module module, String source) {
    myMap.put(module, source);
  }

  @Override
  public MemorySource getSource(Module module) {
    return new MemorySource(myModuleLoader, module, myMap.get(module));
  }
}
