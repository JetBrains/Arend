package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.frontend.LoadingModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.MemoryStorage;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.naming.resolving.SimpleSourceInfoProvider;
import org.junit.Before;

public class StorageTestCase extends VclangTestCase {
  protected LoadingModuleScopeProvider<MemoryStorage.SourceId> moduleLoader;
  protected ModuleScopeProvider moduleScopeProvider;
  protected MemoryStorage storage;
  protected SimpleSourceInfoProvider<MemoryStorage.SourceId> sourceInfoProvider;

  @Before
  public void initialize() {
    moduleLoader = new LoadingModuleScopeProvider<MemoryStorage.SourceId>(errorReporter) {
      @Override
      protected void loadingSucceeded(MemoryStorage.SourceId module, SourceSupplier.LoadResult result) {
        sourceInfoProvider.registerModule(result.group, module);
      }
    };
    moduleScopeProvider = createModuleScopeProvider();
    storage = new MemoryStorage(moduleLoader, moduleScopeProvider);
    moduleLoader.setSourceSupplier(storage);

    sourceInfoProvider = new SimpleSourceInfoProvider<>();
  }

  public ModuleScopeProvider createModuleScopeProvider() {
    return moduleLoader;
  }
}
