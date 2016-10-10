package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

public abstract class SourceModuleLoader<SourceIdT extends SourceId> implements ModuleLoader {
  abstract public SourceIdT locateModule(ModulePath modulePath);
  abstract public SourceSupplier.Result load(SourceIdT sourceId);

  final public Abstract.ClassDefinition load(ModulePath modulePath) {
    SourceSupplier.Result result = load(locateModule(modulePath));
    return result != null ? result.definition : null;
  }
}
