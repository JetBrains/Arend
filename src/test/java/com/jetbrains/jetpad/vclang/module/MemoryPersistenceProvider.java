package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class MemoryPersistenceProvider<SourceIdT extends SourceId> implements PersistenceProvider<SourceIdT> {
  private final Map<String, Object> memMap = new HashMap<>();

  @Override
  public @Nonnull
  URI getUri(SourceIdT sourceId) {
    String key = remember(sourceId);
    return URI.create("memory://" + key);
  }

  @Override
  public @Nullable
  SourceIdT getModuleId(URI sourceUrl) {
    if (!("memory".equals(sourceUrl.getScheme()))) throw new IllegalArgumentException();
    //noinspection unchecked
    return (SourceIdT) recall(sourceUrl.getHost());
  }

  @Override
  public boolean needsCaching(GlobalReferable def, Definition typechecked) {
    return typechecked.status().headerIsOK();
  }

  @Override
  public @Nullable String getIdFor(GlobalReferable definition) {
    return remember(definition);
  }

  @Override
  public @Nonnull GlobalReferable getFromId(SourceIdT sourceId, String id) {
    return (GlobalReferable) recall(id);
  }

  @Override
  public void registerCachedDefinition(SourceIdT sourceId, String id, GlobalReferable parent) {
  }

  private String remember(Object o) {
    String key = objectKey(o);
    Object prev = memMap.put(key, o);
    if (prev != null && !(prev.equals(o))) {
      throw new IllegalStateException();
    }
    return key;
  }

  private Object recall(String key) {
    return memMap.get(key);
  }

  private String objectKey(Object o) {
    return Integer.toString(System.identityHashCode(o));
  }
}
