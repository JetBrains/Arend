package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ModuleLoader<SourceIdT extends SourceId> {
  Abstract.ClassDefinition load(SourceIdT sourceId);
}
