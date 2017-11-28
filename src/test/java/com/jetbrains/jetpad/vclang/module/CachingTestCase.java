package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.StorageTestCase;
import com.jetbrains.jetpad.vclang.frontend.ConcreteReferableProvider;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.caching.CacheLoadingException;
import com.jetbrains.jetpad.vclang.module.caching.CacheManager;
import com.jetbrains.jetpad.vclang.module.caching.CachePersistenceException;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import org.junit.Before;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class CachingTestCase extends StorageTestCase {
  protected CacheManager<MemoryStorage.SourceId> cacheManager;
  protected TypecheckerState tcState;
  private PersistenceProvider<MemoryStorage.SourceId> persistenceProvider = new MemoryPersistenceProvider<>();
  private Typechecking typechecking;

  @Before
  public void initialize() {
    super.initialize();

    // It is a little odd to use the storage itself as a version tracker as it knows nothing about loaded modules
    cacheManager = new CacheManager<>(persistenceProvider, storage, sourceInfoProvider, storage);
    tcState = cacheManager.getTypecheckerState();
    typechecking = new Typechecking(tcState, ConcreteReferableProvider.INSTANCE, errorReporter);
  }

  protected void loadPrelude() {
    ChildGroup prelude = moduleLoader.load(PreludeStorage.PRELUDE_MODULE_PATH);
    new Prelude.PreludeTypechecking(cacheManager.getTypecheckerState()).typecheckModules(Collections.singleton(prelude));
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
}
