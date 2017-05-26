package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface SourceModuleLoader<SourceIdT extends SourceId> {
  SourceIdT locateModule(ModulePath modulePath);
  boolean isAvailable(SourceIdT sourceId);
  Abstract.ClassDefinition load(SourceIdT sourceId);
}
