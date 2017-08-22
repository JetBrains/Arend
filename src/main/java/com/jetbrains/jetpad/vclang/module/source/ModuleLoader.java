package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.term.Concrete;

public interface ModuleLoader<SourceIdT extends SourceId> {
  Concrete.ClassDefinition load(SourceIdT sourceId);
}
