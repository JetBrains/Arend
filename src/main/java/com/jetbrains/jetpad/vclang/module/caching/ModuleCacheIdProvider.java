package com.jetbrains.jetpad.vclang.module.caching;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ModuleCacheIdProvider<SourceIdT> {
  @Nonnull String getCacheId(SourceIdT sourceId);
  @Nullable SourceIdT getModuleId(String cacheID);
}
