package com.jetbrains.jetpad.vclang.module.caching.sourceless;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.SimpleSourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;

public class CacheSourceInfoProvider<SourceIdT extends SourceId> implements SourceInfoProvider<SourceIdT> {
  private final SourceInfoProvider<SourceIdT> mySourceSrcInfoProvider;
  final SimpleSourceInfoProvider<SourceIdT> myCacheSrcInfoProvider = new SimpleSourceInfoProvider<>();

  public CacheSourceInfoProvider(SourceInfoProvider<SourceIdT> sourceInfoProvider) {
    mySourceSrcInfoProvider = sourceInfoProvider;
  }

  @Override
  public SourceIdT sourceOf(GlobalReferable definition) {
    SourceIdT res = mySourceSrcInfoProvider.sourceOf(definition);
    if (res != null) {
      return res;
    } else {
      return myCacheSrcInfoProvider.sourceOf(definition);
    }
  }

  @Override
  public String cacheIdFor(GlobalReferable definition) {
    String res = mySourceSrcInfoProvider.cacheIdFor(definition);
    if (res != null) {
      return res;
    } else {
      return myCacheSrcInfoProvider.cacheIdFor(definition);
    }
  }
}
