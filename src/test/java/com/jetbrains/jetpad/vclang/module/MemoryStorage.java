package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.parser.ParseSource;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.caching.SourceVersionTracker;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MemoryStorage implements Storage<MemoryStorage.SourceId>, SourceVersionTracker<MemoryStorage.SourceId> {
  private Map<ModulePath, Source> mySources = new HashMap<>();
  private Map<ModulePath, ByteArrayOutputStream> myCaches = new HashMap<>();
  private final ModuleRegistry myModuleRegistry;
  private final ModuleScopeProvider myModuleScopeProvider;

  static class Source {
    long version;
    String data;

    Source(String data) {
      this.version = 1;
      this.data = data;
    }
  }

  public MemoryStorage(@Nullable ModuleRegistry moduleRegistry, ModuleScopeProvider moduleScopeProvider) {
    myModuleRegistry = moduleRegistry;
    myModuleScopeProvider = moduleScopeProvider;

    // Be ready to load Prelude
    final String preludeSource;
    InputStream preludeStream = Prelude.class.getResourceAsStream(PreludeStorage.SOURCE_RESOURCE_PATH);
    if (preludeStream == null) {
      throw new IllegalStateException("Prelude source is not available");
    }
    try (Reader in = new InputStreamReader(preludeStream, "UTF-8")) {
      StringBuilder builder = new StringBuilder();
      final char[] buffer = new char[1024 * 1024];
      for (;;) {
        int rsz = in.read(buffer, 0, buffer.length);
        if (rsz < 0)
          break;
        builder.append(buffer, 0, rsz);
      }
      preludeSource = builder.toString();
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    add(PreludeStorage.PRELUDE_MODULE_PATH, preludeSource);
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

  public void removeCache(ModulePath modulePath) {
    ByteArrayOutputStream old = myCaches.remove(modulePath);
    assert old != null;
  }

  public void incVersion(ModulePath modulePath) {
    Source source = mySources.get(modulePath);
    assert source != null;
    source.version += 1;
  }

  @Override
  public SourceId locateModule(@Nonnull ModulePath modulePath) {
    if (mySources.containsKey(modulePath) || myCaches.containsKey(modulePath)) {
      return new SourceId(modulePath);
    } else {
      return null;
    }
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
      ChildGroup result = new ParseSource(sourceId, new StringReader(source.data)) {}.load(errorReporter, myModuleRegistry, myModuleScopeProvider);
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
  public InputStream getCacheInputStream(SourceId sourceId) {
    ByteArrayOutputStream stream = myCaches.get(sourceId.getModulePath());
    return stream != null ? new ByteArrayInputStream(stream.toByteArray()) : null;
  }

  @Override
  public OutputStream getCacheOutputStream(SourceId sourceId) {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    myCaches.put(sourceId.getModulePath(), stream);
    return stream;
  }

  public Snapshot getSnapshot() {
    return new Snapshot(new HashMap<>(mySources), new HashMap<>(myCaches));
  }

  public void restoreSnapshot(Snapshot snapshot) {
    mySources = new HashMap<>(snapshot.mySources);
    myCaches = new HashMap<>(snapshot.myCaches);
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

  public class Snapshot {
    public final Map<ModulePath, Source> mySources;
    public final Map<ModulePath, ByteArrayOutputStream> myCaches;

    private Snapshot(Map<ModulePath, Source> sources, Map<ModulePath, ByteArrayOutputStream> caches) {
      mySources = sources;
      myCaches = caches;
    }
  }
}
