package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.frontend.LoadingModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.MemoryStorage;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import org.junit.Before;

public class StorageTestCase extends VclangTestCase {
  protected LoadingModuleScopeProvider<MemoryStorage.SourceId> moduleLoader;
  protected ModuleScopeProvider moduleScopeProvider;
  protected MemoryStorage storage;

  @Before
  public void initialize() {
    moduleLoader = new LoadingModuleScopeProvider<MemoryStorage.SourceId>(errorReporter) {
      @Override
      protected void loadingSucceeded(MemoryStorage.SourceId module, SourceSupplier.LoadResult result) {
        StorageTestCase.this.loadingSucceeded(module, result);
      }
    };
    moduleScopeProvider = moduleLoader;
    storage = new MemoryStorage(moduleLoader, moduleLoader);
    moduleLoader.setSourceSupplier(storage);
  }

  protected void loadingSucceeded(MemoryStorage.SourceId module, SourceSupplier.LoadResult result) {}
}
