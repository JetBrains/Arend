package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.namespace.ModuleRegistry;
import com.jetbrains.jetpad.vclang.frontend.text.parser.ParseSource;
import com.jetbrains.jetpad.vclang.module.caching.SourceVersionTracker;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MemoryStorage implements Storage<MemoryStorage.SourceId>, SourceVersionTracker<MemoryStorage.SourceId> {
  private final Map<ModulePath, Source> mySources = new HashMap<>();
  private final Map<SourceId, ByteArrayOutputStream> myCaches = new HashMap<>();
  private final ModuleRegistry myModuleRegistry;
  private Scope myGlobalScope = new EmptyScope();
  private final NameResolver myNameResolver;

  static class Source {
    long version;
    String data;

    Source(String data) {
      this.version = 1;
      this.data = data;
    }
  }

  public MemoryStorage(ModuleRegistry moduleRegistry, NameResolver nameResolver) {
    myModuleRegistry = moduleRegistry;
    myNameResolver = nameResolver;
  }

  public void setPreludeNamespace(Namespace ns) {
    myGlobalScope = new NamespaceScope(ns);
  }

  public SourceId add(ModulePath modulePath, String source) {
    Source old = mySources.put(modulePath, new Source(source));
    assert old == null;
    return locateModule(modulePath);
  }

  public void remove(ModulePath modulePath) {
    Source old = mySources.remove(modulePath);
    assert old != null;
  }

  public void incVersion(ModulePath modulePath) {
    Source source = mySources.get(modulePath);
    assert source != null;
    source.version += 1;
  }

  @Override
  public SourceId locateModule(@Nonnull ModulePath modulePath) {
    Source source = mySources.get(modulePath);
    return source != null ? new SourceId(modulePath) : null;
  }

  @Override
  public boolean isAvailable(@Nonnull SourceId sourceId) {
    return mySources.containsKey(sourceId.getModulePath());
  }

  @Override
  public LoadResult loadSource(@Nonnull SourceId sourceId, @Nonnull ErrorReporter errorReporter) {
    if (!isAvailable(sourceId)) return null;
    try {
      Source source = mySources.get(sourceId.getModulePath());
      Abstract.ClassDefinition result = new ParseSource(sourceId, new StringReader(source.data)) {}.load(
          errorReporter, myModuleRegistry, myGlobalScope, myNameResolver);
      return LoadResult.make(result, source.version);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public long getAvailableVersion(@Nonnull SourceId sourceId) {
    return mySources.get(sourceId.getModulePath()).version;
  }

  @Override
  public long getCurrentVersion(@Nonnull SourceId sourceId) {
    return getAvailableVersion(sourceId);
  }

  @Override
  public boolean ensureLoaded(@Nonnull SourceId sourceId, long version) {
    return getCurrentVersion(sourceId) == version;
  }

  @Override
  public InputStream getCacheInputStream(SourceId sourceId) {
    ByteArrayOutputStream stream = myCaches.get(sourceId);
    return stream != null ? new ByteArrayInputStream(stream.toByteArray()) : null;
  }

  @Override
  public OutputStream getCacheOutputStream(SourceId sourceId) {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    myCaches.put(sourceId, stream);
    return stream;
  }

  public class SourceId implements com.jetbrains.jetpad.vclang.module.source.SourceId {
    private final ModulePath myPath;

    private SourceId(ModulePath path) {
      myPath = path;
    }

    @Override
    public ModulePath getModulePath() {
      return myPath;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SourceId sourceId = (SourceId) o;

      return myPath != null ? myPath.equals(sourceId.myPath) : sourceId.myPath == null;
    }

    @Override
    public int hashCode() {
      return myPath != null ? myPath.hashCode() : 0;
    }
  }
}
