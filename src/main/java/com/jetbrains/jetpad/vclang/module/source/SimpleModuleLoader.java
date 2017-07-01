package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class SimpleModuleLoader<SourceIdT extends SourceId> implements SourceModuleLoader<SourceIdT> {
  private final SourceSupplier<SourceIdT> mySourceSupplier;
  protected final ErrorReporter myErrorReporter;

  public SimpleModuleLoader(SourceSupplier<SourceIdT> sourceSupplier, ErrorReporter errorReporter) {
    mySourceSupplier = sourceSupplier;
    myErrorReporter = errorReporter;
  }


  @Override
  public SourceIdT locateModule(ModulePath modulePath) {
    return mySourceSupplier.locateModule(modulePath);
  }

  @Override
  public boolean isAvailable(SourceIdT sourceId) {
    return mySourceSupplier.isAvailable(sourceId);
  }

  @Override
  public Abstract.ClassDefinition load(SourceIdT sourceId) {
    if (!mySourceSupplier.isAvailable(sourceId)) {
      throw new IllegalStateException("Source <" + sourceId + "> is not available");
    }

    return mySourceSupplier.loadSource(sourceId, myErrorReporter);
  }
}
