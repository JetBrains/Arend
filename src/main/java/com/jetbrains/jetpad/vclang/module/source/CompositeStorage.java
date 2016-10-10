package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;

import java.io.InputStream;
import java.io.OutputStream;

public class CompositeStorage<SourceId1T extends SourceId, SourceId2T extends SourceId> extends CompositeSourceSupplier<SourceId1T, SourceId2T> implements CacheStorageSupplier<CompositeSourceSupplier<SourceId1T, SourceId2T>.SourceId> {
  private final CacheStorageSupplier<SourceId1T> mySup1;
  private final CacheStorageSupplier<SourceId2T> mySup2;

  public CompositeStorage(SourceSupplier<SourceId1T> sSupplier1, SourceSupplier<SourceId2T> sSupplier2, CacheStorageSupplier<SourceId1T> csSupplier1, CacheStorageSupplier<SourceId2T> csSupplier2) {
    super(sSupplier1, sSupplier2);
    mySup1 = csSupplier1;
    mySup2 = csSupplier2;
  }

  @Override
  public InputStream getCacheInputStream(SourceId sourceId) {
    if (sourceId.getSourceSupplier() != this) return null;
    if (sourceId.source1 != null) {
      return mySup1.getCacheInputStream(sourceId.source1);
    } else {
      return mySup2.getCacheInputStream(sourceId.source2);
    }
  }

  @Override
  public OutputStream getCacheOutputStream(SourceId sourceId) {
    if (sourceId.getSourceSupplier() != this) return null;
    if (sourceId.source1 != null) {
      return mySup1.getCacheOutputStream(sourceId.source1);
    } else {
      return mySup2.getCacheOutputStream(sourceId.source2);
    }
  }

  @Override
  public byte[] getCurrentEtag(SourceId sourceId) {
    if (sourceId.getSourceSupplier() != this) return null;
    if (sourceId.source1 != null) {
      return mySup1.getCurrentEtag(sourceId.source1);
    } else {
      return mySup2.getCurrentEtag(sourceId.source2);
    }
  }
}
