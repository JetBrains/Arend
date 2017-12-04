package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

public interface CacheIdProvider {
  String cacheIdFor(GlobalReferable definition);
}
