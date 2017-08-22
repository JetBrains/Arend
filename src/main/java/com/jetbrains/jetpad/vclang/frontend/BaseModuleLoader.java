package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.naming.ModuleResolver;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class BaseModuleLoader<SourceIdT extends SourceId> implements ModuleLoader<SourceIdT>, ModuleResolver {
  protected final Storage<SourceIdT> myStorage;
  private final ErrorReporter myErrorReporter;

  public BaseModuleLoader(Storage<SourceIdT> storage, ErrorReporter errorReporter) {
    myStorage = storage;
    myErrorReporter = errorReporter;
  }


  protected void loadingSucceeded(SourceIdT module, SourceSupplier.LoadResult result) {}

  protected void loadingFailed(SourceIdT module) {}

  @Override
  public Concrete.ClassDefinition load(SourceIdT sourceId) {
    final SourceSupplier.LoadResult result;
    result = myStorage.loadSource(sourceId, myErrorReporter);

    if (result != null) {
      loadingSucceeded(sourceId, result);
      return result.definition;
    } else {
      loadingFailed(sourceId);
      return null;
    }
  }

  @Override
  public Concrete.ClassDefinition load(ModulePath modulePath) {
    return load(locateModule(modulePath));
  }

  public SourceIdT locateModule(ModulePath modulePath) {
    SourceIdT sourceId = myStorage.locateModule(modulePath);
    if (sourceId == null) throw new IllegalStateException();
    return sourceId;
  }
}
