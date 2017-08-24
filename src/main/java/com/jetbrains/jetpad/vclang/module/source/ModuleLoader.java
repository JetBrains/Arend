package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.term.Group;

public interface ModuleLoader<SourceIdT extends SourceId> {
  Group load(SourceIdT sourceId);
}
