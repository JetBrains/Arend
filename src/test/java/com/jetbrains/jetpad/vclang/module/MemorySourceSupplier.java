package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.DummySource;
import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.util.HashMap;
import java.util.Map;

public class MemorySourceSupplier implements SourceSupplier {
  private final ModuleLoader myModuleLoader;
  private final ErrorReporter myErrorReporter;
  private final Map<ResolvedName, Source> myMap = new HashMap<>();

  public MemorySourceSupplier(ModuleLoader moduleLoader, ErrorReporter errorReporter) {
    myModuleLoader = moduleLoader;
    myErrorReporter = errorReporter;
    addContainer(RootModule.ROOT.getResolvedName());
  }

  public void add(ResolvedName module, String source) {
    Source parentSoucre = getSource(module.parent.getResolvedName());
    if (parentSoucre instanceof MemorySource) {
      ((MemorySource) parentSoucre).addChild(module.name.name);
    } else if (parentSoucre instanceof ContainerSource) {
      ((ContainerSource) parentSoucre).addChild(module.name.name);
    } else {
      throw new IllegalStateException();
    }

    myMap.put(module, new MemorySource(myModuleLoader, myErrorReporter, module, source));
  }

  public void addContainer(ResolvedName module) {
    myMap.put(module, new ContainerSource(module));
  }

  @Override
  public Source getSource(ResolvedName module) {
    return myMap.containsKey(module) ? myMap.get(module) : new DummySource();
  }
}
