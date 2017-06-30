package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleModuleNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.frontend.resolving.NamespaceProviders;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.caching.CacheManager;
import com.jetbrains.jetpad.vclang.module.caching.CachePersistenceException;
import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.DefinitionLocator;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;

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
    public PreludeStorage.SourceId sourceOf(Abstract.Definition definition) {
      return preludeSourceId;
    }
  }

  static class PreludePersistenceProvider implements PersistenceProvider<PreludeStorage.SourceId> {
    @Override
    public URI getUri(PreludeStorage.SourceId sourceId) {
      throw new IllegalStateException();
    }

    @Override
    public PreludeStorage.SourceId getModuleId(URI sourceUrl) {
      throw new IllegalStateException();
    }

    @Override
    public String getIdFor(Abstract.Definition definition) {
      if (!(definition instanceof Concrete.Definition)) throw new IllegalStateException();
      Concrete.Position pos = ((Concrete.Definition) definition).getPosition();
      if (pos == null) throw new IllegalStateException();
      return pos.line + ";" + pos.column;
    }

    @Override
    public Abstract.Definition getFromId(PreludeStorage.SourceId sourceId, String id) {
      throw new IllegalStateException();
    }
  }

  public static void main(String[] args) throws IOException, CachePersistenceException {
    final ListErrorReporter errorReporter = new ListErrorReporter();
    final StaticNamespaceProvider staticNsProvider = new SimpleStaticNamespaceProvider();
    final DynamicNamespaceProvider dynamicNsProvider = new SimpleDynamicNamespaceProvider();
    final NameResolver nameResolver = new NameResolver(new NamespaceProviders(new SimpleModuleNamespaceProvider(), staticNsProvider, dynamicNsProvider));

    PreludeStorage storage = new PreludeStorage(nameResolver);
    CacheManager<PreludeStorage.SourceId> cacheManager = new CacheManager<>(new PreludePersistenceProvider(), new PreludeBuildCacheSupplier(Paths.get(args[0])), new PreludeDefLocator(storage.preludeSourceId));

    Abstract.ClassDefinition prelude = storage.loadSource(storage.preludeSourceId, errorReporter);
    if (!errorReporter.getErrorList().isEmpty()) throw new IllegalStateException();
    new Typechecking(cacheManager.getTypecheckerState(), staticNsProvider, dynamicNsProvider, errorReporter, new Prelude.UpdatePreludeReporter(cacheManager.getTypecheckerState()), new DependencyListener() {}).typecheckModules(Collections.singleton(prelude));

    cacheManager.persistCache(storage.preludeSourceId);
  }
}
