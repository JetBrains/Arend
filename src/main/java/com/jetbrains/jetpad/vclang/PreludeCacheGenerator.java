package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.module.BaseModuleLoader;
import com.jetbrains.jetpad.vclang.module.caching.CachePersistenceException;
import com.jetbrains.jetpad.vclang.module.caching.CacheStorageSupplier;
import com.jetbrains.jetpad.vclang.module.caching.CachingModuleLoader;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.file.FileStorage;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.oneshot.ResolvingModuleLoader;
import com.jetbrains.jetpad.vclang.term.*;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;

import java.io.*;
import java.net.URL;
import java.util.Collections;

public class PreludeCacheGenerator {
  private static class PreludeBuildCacheSupplier implements CacheStorageSupplier<Prelude.SourceId> {
    private final String targetPath;

    private PreludeBuildCacheSupplier(String targetPath) {
      this.targetPath = targetPath;
    }

    @Override
    public InputStream getCacheInputStream(Prelude.SourceId sourceId) {
      throw new IllegalStateException();
    }

    @Override
    public OutputStream getCacheOutputStream(Prelude.SourceId sourceId) {
      String path = targetPath + Prelude.PreludeStorage.SOURCE_RESOURCE_PATH + FileStorage.SERIALIZED_EXTENSION;
      File file = new File(path);
      //noinspection ResultOfMethodCallIgnored
      file.getParentFile().mkdirs();
      try {
        return new FileOutputStream(file);
      } catch (FileNotFoundException e) {
        throw new IllegalStateException();
      }
    }
  }

  private static class PreludeDefLocator implements DefinitionLocator<Prelude.SourceId> {
    private final Prelude.SourceId preludeSourceId;

    private PreludeDefLocator(Prelude.SourceId preludeSourceId) {
      this.preludeSourceId = preludeSourceId;
    }

    @Override
    public Prelude.SourceId sourceOf(Abstract.Definition definition) {
      return preludeSourceId;
    }
  }

  static class PreludePersistenceProvider implements PersistenceProvider<Prelude.SourceId> {
    @Override
    public URL getUrl(Prelude.SourceId sourceId) {
      throw new IllegalStateException();
    }

    @Override
    public Prelude.SourceId getModuleId(URL sourceUrl) {
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
    public Abstract.Definition getFromId(Prelude.SourceId sourceId, String id) {
      throw new IllegalStateException();
    }
  }

  public static void main(String[] args) {
    final StaticNamespaceProvider statisNsProvider = new SimpleStaticNamespaceProvider();
    final DynamicNamespaceProvider dynamicNsProvider = new SimpleDynamicNamespaceProvider();
    final ListErrorReporter errorReporter = new ListErrorReporter();
    Prelude.PreludeStorage storage = new Prelude.PreludeStorage();
    ResolvingModuleLoader<Prelude.SourceId> baseModuleLoader = new ResolvingModuleLoader<>(storage, new BaseModuleLoader.ModuleLoadingListener<Prelude.SourceId>(), statisNsProvider, dynamicNsProvider, new ConcreteResolveListener(), errorReporter);
    CachingModuleLoader<Prelude.SourceId> moduleLoader = new CachingModuleLoader<>(baseModuleLoader, new PreludePersistenceProvider(), new PreludeBuildCacheSupplier(args[0]), new PreludeDefLocator(storage.preludeSourceId), false);
    Abstract.ClassDefinition prelude = moduleLoader.load(storage.preludeSourceId);
    if (!errorReporter.getErrorList().isEmpty()) throw new IllegalStateException();
    Typechecking.typecheckModules(moduleLoader.getTypecheckerState(), statisNsProvider, dynamicNsProvider, Collections.singleton(prelude), errorReporter, new Prelude.UpdatePreludeReporter(moduleLoader.getTypecheckerState()));
    try {
      moduleLoader.persistModule(storage.preludeSourceId);
    } catch (IOException | CachePersistenceException e) {
      throw new IllegalStateException();
    }
  }
}
