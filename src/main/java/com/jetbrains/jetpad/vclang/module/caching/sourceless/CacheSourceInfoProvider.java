package com.jetbrains.jetpad.vclang.module.caching.sourceless;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
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
  public FullName fullNameFor(GlobalReferable definition) {
    FullName res = mySourceSrcInfoProvider.fullNameFor(definition);
    if (res != null) {
      return res;
    } else {
      return myCacheSrcInfoProvider.fullNameFor(definition);
    }
  }

  @Override
  public String nameFor(Referable referable) {
    String res = mySourceSrcInfoProvider.nameFor(referable);
    if (res != null) {
      return res;
    } else {
      return myCacheSrcInfoProvider.nameFor(referable);
    }
  }
}
