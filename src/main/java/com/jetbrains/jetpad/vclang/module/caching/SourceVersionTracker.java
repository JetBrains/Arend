package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.module.source.SourceId;

import javax.annotation.Nonnull;

public interface SourceVersionTracker<SourceIdT extends SourceId> {
  long getCurrentVersion(@Nonnull SourceIdT sourceId);
}
