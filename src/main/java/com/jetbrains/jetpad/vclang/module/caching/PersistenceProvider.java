package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface PersistenceProvider<SourceIdT extends SourceId> extends ModuleUriProvider<SourceIdT> {
  default boolean needsCaching(GlobalReferable def, Definition typechecked) {
    return true;
  }
  @Nullable String getIdFor(GlobalReferable definition);
  @Nonnull GlobalReferable getFromId(SourceIdT sourceId, String id);
  void registerCachedDefinition(SourceIdT sourceId, String id, GlobalReferable parent);
}
