package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.BaseModuleLoader;
import com.jetbrains.jetpad.vclang.frontend.ConcreteTypecheckableProvider;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.frontend.resolving.HasOpens;
import com.jetbrains.jetpad.vclang.frontend.resolving.OneshotSourceInfoCollector;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.caching.CacheLoadingException;
import com.jetbrains.jetpad.vclang.module.caching.CacheManager;
import com.jetbrains.jetpad.vclang.module.caching.CachePersistenceException;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class CachingTestCase extends NameResolverTestCase {
  protected final MemoryStorage storage = new MemoryStorage(moduleNsProvider, nameResolver);
  protected final List<Abstract.Definition> typecheckingSucceeded = new ArrayList<>();
  protected final List<Abstract.Definition> typecheckingFailed = new ArrayList<>();
  protected BaseModuleLoader<MemoryStorage.SourceId> moduleLoader;
  protected CacheManager<MemoryStorage.SourceId> cacheManager;
  protected TypecheckerState tcState;
  private OneshotSourceInfoCollector<MemoryStorage.SourceId> srcInfoCollector;
  private PersistenceProvider<MemoryStorage.SourceId> peristenceProvider = new MemoryPersistenceProvider<>();
  private Typechecking<Position> typechecking;

  @Before
  public void initialize() {
    srcInfoCollector = new OneshotSourceInfoCollector<>();
    moduleLoader = new BaseModuleLoader<MemoryStorage.SourceId>(storage, errorReporter) {
      @Override
      protected void loadingSucceeded(MemoryStorage.SourceId module, SourceSupplier.LoadResult result) {
        if (result == null) throw new IllegalStateException("Could not load module");
        srcInfoCollector.visitModule(module, (Concrete.ClassDefinition) result.definition);
      }
    };
    nameResolver.setModuleResolver(moduleLoader);
    // It is a little odd to use the storage itself as a version tracker as it knows nothing about loaded modules
    cacheManager = new CacheManager<>(peristenceProvider, storage, storage, srcInfoCollector.sourceInfoProvider);
    tcState = cacheManager.getTypecheckerState();
    typechecking = new Typechecking<>(tcState, staticNsProvider, dynamicNsProvider, HasOpens.GET, ConcreteTypecheckableProvider.INSTANCE, errorReporter, new TypecheckedReporter<Position>() {
      @Override
      public void typecheckingSucceeded(Concrete.Definition<Position> definition) {
        typecheckingSucceeded.add(definition);
      }
      @Override
      public void typecheckingFailed(Concrete.Definition<Position> definition) {
        typecheckingFailed.add(definition);
      }
    }, new DependencyListener<Position>() {});
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

    storage.add(ModulePath.moduleName("Prelude"), preludeSource);
    MemoryStorage.SourceId sourceId = storage.locateModule(ModulePath.moduleName("Prelude"));

    prelude = (Concrete.ClassDefinition<Position>) moduleLoader.load(sourceId);
    new Typechecking<>(cacheManager.getTypecheckerState(), staticNsProvider, dynamicNsProvider, HasOpens.GET, ConcreteTypecheckableProvider.INSTANCE, new DummyErrorReporter<>(), new Prelude.UpdatePreludeReporter<>(cacheManager.getTypecheckerState()), new DependencyListener<Position>() {}).typecheckModules(Collections.singleton(this.prelude));
    storage.setPreludeNamespace(staticNsProvider.forReferable(prelude));
  }

  protected void typecheck(Concrete.ClassDefinition<Position> module) {
    typecheck(module, 0);
  }

  protected void typecheck(Concrete.ClassDefinition<Position> module, int size) {
    typechecking.typecheckModules(Collections.singleton(module));
    assertThat(errorList, size > 0 ? hasSize(size) : is(empty()));
  }

  protected void tryLoad(MemoryStorage.SourceId sourceId, GlobalReferable classDefinition, boolean shouldLoad) throws CacheLoadingException {
    boolean loaded = cacheManager.loadCache(sourceId, classDefinition);
    assertThat(loaded, is(shouldLoad));
    assertThat(errorList, is(empty()));
  }

  protected void load(MemoryStorage.SourceId sourceId, GlobalReferable classDefinition) {
    try {
      tryLoad(sourceId, classDefinition, true);
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
    public URI getUri(SourceIdT sourceId) {
      String key = remember(sourceId);
      return URI.create("memory://" + key);
    }

    @Override
    public SourceIdT getModuleId(URI sourceUrl) {
      if (!("memory".equals(sourceUrl.getScheme()))) throw new IllegalArgumentException();
      //noinspection unchecked
      return (SourceIdT) recall(sourceUrl.getHost());
    }

    @Override
    public String getIdFor(GlobalReferable definition) {
      return remember(definition);
    }

    @Override
    public GlobalReferable getFromId(SourceIdT sourceId, String id) {
      return (GlobalReferable) recall(id);
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
