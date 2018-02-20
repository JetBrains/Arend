package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;

public interface ModuleLoader<SourceIdT extends SourceId> {
  ChildGroup load(SourceIdT sourceId);
  SourceIdT locateModule(ModulePath modulePath);
}
