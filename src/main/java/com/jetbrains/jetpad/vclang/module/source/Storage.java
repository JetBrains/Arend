package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;

public interface Storage<SourceIdT extends SourceId> extends SourceSupplier<SourceIdT>, CacheStorageSupplier<SourceIdT> {
}
