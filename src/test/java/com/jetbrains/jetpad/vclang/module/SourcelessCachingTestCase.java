package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.StorageTestCase;
import com.jetbrains.jetpad.vclang.frontend.ConcreteReferableProvider;
import com.jetbrains.jetpad.vclang.module.caching.CacheManager;
import com.jetbrains.jetpad.vclang.module.caching.CachePersistenceException;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.caching.sourceless.CacheModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.caching.sourceless.CacheSourceInfoProvider;
import com.jetbrains.jetpad.vclang.module.caching.sourceless.SourcelessCacheManager;
import com.jetbrains.jetpad.vclang.module.scopeprovider.EmptyModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import org.junit.Before;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class SourcelessCachingTestCase extends StorageTestCase {
  protected CacheModuleScopeProvider<MemoryStorage.SourceId> cacheModuleScopeProvider;
  protected CacheSourceInfoProvider<MemoryStorage.SourceId> cacheSourceInfoProvider;
  protected CacheManager<MemoryStorage.SourceId> cacheManager;
  protected TypecheckerState tcState;
  private PersistenceProvider<MemoryStorage.SourceId> persistenceProvider = new MemoryPersistenceProvider<>();
  private Typechecking typechecking;

  @Before
  public void initialize() {
    super.initialize();

    cacheSourceInfoProvider = new CacheSourceInfoProvider<>(sourceInfoProvider);

    // It is a little odd to use the storage itself as a version tracker as it knows nothing about loaded modules
    cacheManager = new SourcelessCacheManager<>(storage, persistenceProvider, EmptyModuleScopeProvider.INSTANCE, cacheModuleScopeProvider, cacheSourceInfoProvider, storage);
    cacheModuleScopeProvider.initialise(storage, cacheManager);

    tcState = cacheManager.getTypecheckerState();
    typechecking = new Typechecking(tcState, ConcreteReferableProvider.INSTANCE, errorReporter);
  }

  protected void typecheck(Group module) {
    typecheck(module, 0);
  }

  protected void typecheck(Group module, int size) {
    typechecking.typecheckModules(Collections.singleton(module));
    assertThat(errorList, size > 0 ? hasSize(size) : is(empty()));
  }

  protected void load(ModulePath module) {
    cacheModuleScopeProvider.forModule(module);
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

  @Override
  public ModuleScopeProvider createModuleScopeProvider() {
    cacheModuleScopeProvider = new CacheModuleScopeProvider<>(moduleLoader);
    return cacheModuleScopeProvider;
  }
}
