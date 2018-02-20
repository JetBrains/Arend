package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;

public class BaseModuleLoader<SourceIdT extends SourceId> implements ModuleLoader<SourceIdT> {
  protected SourceSupplier<SourceIdT> mySourceSupplier;
  private final ErrorReporter myErrorReporter;

  public BaseModuleLoader(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  public void setSourceSupplier(SourceSupplier<SourceIdT> sourceSupplier) {
    mySourceSupplier = sourceSupplier;
  }

  protected void loadingSucceeded(SourceIdT module, SourceSupplier.LoadResult result) {}

  protected void loadingFailed(SourceIdT module) {}

  @Override
  public ChildGroup load(SourceIdT sourceId) {
    final SourceSupplier.LoadResult result;
    result = mySourceSupplier.loadSource(sourceId, myErrorReporter);

    if (result != null) {
      loadingSucceeded(sourceId, result);
      return result.group;
    } else {
      loadingFailed(sourceId);
      return null;
    }
  }

  public ChildGroup load(ModulePath modulePath) {
    SourceIdT sourceId = locateModule(modulePath);
    if (sourceId != null) {
      return load(sourceId);
    } else {
      return null;
    }
  }

  @Override
  public SourceIdT locateModule(ModulePath modulePath) {
    return mySourceSupplier.locateModule(modulePath);
  }
}
