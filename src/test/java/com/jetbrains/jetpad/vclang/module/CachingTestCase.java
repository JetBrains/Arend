package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.BaseModuleLoader;
import com.jetbrains.jetpad.vclang.frontend.ConcreteReferableProvider;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.caching.CacheLoadingException;
import com.jetbrains.jetpad.vclang.module.caching.CacheManager;
import com.jetbrains.jetpad.vclang.module.caching.CachePersistenceException;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.SimpleSourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
import org.junit.Before;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class CachingTestCase extends NameResolverTestCase {
  protected MemoryStorage storage;
  protected BaseModuleLoader<MemoryStorage.SourceId> moduleLoader;
  protected CacheManager<MemoryStorage.SourceId> cacheManager;
  protected TypecheckerState tcState;
  private SimpleSourceInfoProvider<MemoryStorage.SourceId> sourceInfoProvider;
  private PersistenceProvider<MemoryStorage.SourceId> persistenceProvider = new MemoryPersistenceProvider<>();
  private Typechecking typechecking;

  @Before
  public void initialize() {
    sourceInfoProvider = new SimpleSourceInfoProvider<>();
    moduleLoader = new BaseModuleLoader<MemoryStorage.SourceId>(errorReporter) {
      @Override
      protected void loadingSucceeded(MemoryStorage.SourceId module, SourceSupplier.LoadResult result) {
        if (result == null) throw new IllegalStateException("Could not load module");
        sourceInfoProvider.registerModule(result.group, module);
      }
    };
    storage = new MemoryStorage(moduleScopeProvider, moduleLoader, moduleScopeProvider);
    moduleLoader.setStorage(storage);
    // It is a little odd to use the storage itself as a version tracker as it knows nothing about loaded modules
    cacheManager = new CacheManager<>(persistenceProvider, storage, sourceInfoProvider, storage);
    tcState = cacheManager.getTypecheckerState();
    typechecking = new Typechecking(tcState, ConcreteReferableProvider.INSTANCE, errorReporter, TypecheckedReporter.DUMMY, new DependencyListener() {});
  }

  @Override
  protected void loadPrelude() {
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

    storage.add(PreludeStorage.PRELUDE_MODULE_PATH, preludeSource);
    MemoryStorage.SourceId sourceId = storage.locateModule(PreludeStorage.PRELUDE_MODULE_PATH);

    prelude = moduleLoader.load(sourceId);
    new Typechecking(cacheManager.getTypecheckerState(), ConcreteReferableProvider.INSTANCE, DummyErrorReporter.INSTANCE, new Prelude.UpdatePreludeReporter(), new DependencyListener() {}).typecheckModules(Collections.singleton(this.prelude));
  }

  protected void typecheck(Group module) {
    typecheck(module, 0);
  }

  protected void typecheck(Group module, int size) {
    typechecking.typecheckModules(Collections.singleton(module));
    assertThat(errorList, size > 0 ? hasSize(size) : is(empty()));
  }

  protected void tryLoad(MemoryStorage.SourceId sourceId, boolean shouldLoad) throws CacheLoadingException {
    boolean loaded = cacheManager.loadCache(sourceId);
    assertThat(loaded, is(shouldLoad));
    assertThat(errorList, is(empty()));
  }

  protected void load(MemoryStorage.SourceId sourceId) {
    try {
      tryLoad(sourceId, true);
    } catch (CacheLoadingException e) {
      fail(e.toString());
    }
  }

  protected void persist(MemoryStorage.SourceId sourceId) {
    try {
      boolean persisted = cacheManager.persistCache(sourceId);
      assertThat(persisted, is(true));
    } catch (CachePersistenceException e) {
      throw new IllegalStateException();
    }
    assertThat(errorList, is(empty()));
  }


  public class MemoryPersistenceProvider<SourceIdT extends SourceId> implements PersistenceProvider<SourceIdT> {
    private final Map<String, Object> memMap = new HashMap<>();

    @Override
    public @Nonnull URI getUri(SourceIdT sourceId) {
      String key = remember(sourceId);
      return URI.create("memory://" + key);
    }

    @Override
    public @Nullable SourceIdT getModuleId(URI sourceUrl) {
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
    public void registerCachedDefinition(SourceIdT sourceId, String id, Definition definition) {
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
}
