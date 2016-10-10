package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.source.SourceId;

public interface DefinitionLocator<SourceIdT extends SourceId> {
  SourceIdT sourceOf(Abstract.Definition definition);
}
