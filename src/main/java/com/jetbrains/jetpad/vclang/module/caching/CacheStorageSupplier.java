package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.module.source.SourceId;

import java.io.InputStream;
import java.io.OutputStream;

public interface CacheStorageSupplier<SourceIdT extends SourceId> {
  InputStream getCacheInputStream(SourceIdT sourceId);
  OutputStream getCacheOutputStream(SourceIdT sourceId);
  byte[] getCurrentEtag(SourceIdT sourceId);
}
