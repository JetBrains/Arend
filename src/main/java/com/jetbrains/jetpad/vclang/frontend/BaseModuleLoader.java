package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Storage;
import com.jetbrains.jetpad.vclang.module.ModuleResolver;
import com.jetbrains.jetpad.vclang.term.ChildGroup;

public class BaseModuleLoader<SourceIdT extends SourceId> implements ModuleLoader<SourceIdT>, ModuleResolver {
  protected Storage<SourceIdT> myStorage;
  private final ErrorReporter myErrorReporter;

  public BaseModuleLoader(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  public void setStorage(Storage<SourceIdT> storage) {
    myStorage = storage;
  }

  protected void loadingSucceeded(SourceIdT module, SourceSupplier.LoadResult result) {}

  protected void loadingFailed(SourceIdT module) {}

  @Override
  public ChildGroup load(SourceIdT sourceId) {
    final SourceSupplier.LoadResult result;
    result = myStorage.loadSource(sourceId, myErrorReporter);

    if (result != null) {
      loadingSucceeded(sourceId, result);
      return result.group;
    } else {
      loadingFailed(sourceId);
      return null;
    }
  }

  @Override
  public boolean load(ModulePath modulePath) {
    SourceIdT sourceId = locateModule(modulePath);
    return sourceId != null && load(sourceId) != null;
  }

  public SourceIdT locateModule(ModulePath modulePath) {
    return myStorage.locateModule(modulePath);
  }
}
