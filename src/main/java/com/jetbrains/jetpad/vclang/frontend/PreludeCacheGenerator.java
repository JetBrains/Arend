package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.caching.*;
import com.jetbrains.jetpad.vclang.module.caching.sourceless.SourcelessCacheManager;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.SimpleSourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.DefinitionLocator;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.provider.FullNameProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class PreludeCacheGenerator {
  private static class PreludeBuildCacheSupplier implements CacheStorageSupplier<PreludeStorage.SourceId> {
    private final Path targetPath;

    private PreludeBuildCacheSupplier(Path targetPath) {
      this.targetPath = targetPath;
    }

    @Override
    public InputStream getCacheInputStream(PreludeStorage.SourceId sourceId) {
      throw new IllegalStateException();
    }

    @Override
    public OutputStream getCacheOutputStream(PreludeStorage.SourceId sourceId) {
      Path path = targetPath.resolve(PreludeStorage.CACHE_PATH);
      try {
        Files.createDirectories(path.getParent());
        return Files.newOutputStream(path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class PreludeDefLocator implements DefinitionLocator<PreludeStorage.SourceId> {
    private final PreludeStorage.SourceId preludeSourceId;

    private PreludeDefLocator(PreludeStorage.SourceId preludeSourceId) {
      this.preludeSourceId = preludeSourceId;
    }

    @Override
    public PreludeStorage.SourceId sourceOf(GlobalReferable definition) {
      return preludeSourceId;
    }
  }

  static class PreludePersistenceProvider implements PersistenceProvider<PreludeStorage.SourceId> {
    private final FullNameProvider myFullNameProvider;

    PreludePersistenceProvider(FullNameProvider fullNameProvider) {
      myFullNameProvider = fullNameProvider;
    }

    @Override
    public @Nonnull URI getUri(PreludeStorage.SourceId sourceId) {
      throw new IllegalStateException();
    }

    @Override
    public @Nonnull PreludeStorage.SourceId getModuleId(URI sourceUrl) {
      throw new IllegalStateException();
    }

    @Override
    public boolean needsCaching(GlobalReferable def, Definition typechecked) {
      return true;
    }

    @Override
    public @Nullable String getIdFor(GlobalReferable definition) {
      return SourcelessCacheManager.getNameIdFor(myFullNameProvider, definition);
    }

    @Override
    public @Nonnull GlobalReferable getFromId(PreludeStorage.SourceId sourceId, String id) {
      throw new IllegalStateException();
    }

    @Override
    public void registerCachedDefinition(PreludeStorage.SourceId sourceId, String id, GlobalReferable parent) {
      throw new IllegalStateException();
    }
  }

  static class PreludeVersionTracker implements SourceVersionTracker<PreludeStorage.SourceId> {
    @Override
    public long getCurrentVersion(@Nonnull PreludeStorage.SourceId sourceId) {
      return 1;
    }
  }

  public static void main(String[] args) throws CachePersistenceException {
    PreludeStorage storage = new PreludeStorage(null);
    SimpleSourceInfoProvider<PreludeStorage.SourceId> sourceInfoProvider = new SimpleSourceInfoProvider<>();
    CacheManager<PreludeStorage.SourceId> cacheManager = new CacheManager<>(new PreludePersistenceProvider(sourceInfoProvider), new PreludeBuildCacheSupplier(Paths.get(args[0])),
        new PreludeDefLocator(storage.preludeSourceId), new PreludeVersionTracker());

    final ListErrorReporter errorReporter = new ListErrorReporter();
    Group prelude = storage.loadSource(storage.preludeSourceId, errorReporter).group;
    if (!errorReporter.getErrorList().isEmpty()) throw new IllegalStateException();
    sourceInfoProvider.registerModule(prelude, storage.preludeSourceId);
    new Prelude.PreludeTypechecking(cacheManager.getTypecheckerState()).typecheckModules(Collections.singleton(prelude));

    cacheManager.persistCache(storage.preludeSourceId);
  }
}
