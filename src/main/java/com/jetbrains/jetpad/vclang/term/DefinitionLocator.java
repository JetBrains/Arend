package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

public interface DefinitionLocator<SourceIdT extends SourceId> {
  SourceIdT sourceOf(GlobalReferable definition);
}
