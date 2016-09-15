package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;

public abstract class ReportingModuleLoader<ModuleSourceIdT extends ModuleSourceId> extends ModuleLoader<ModuleSourceIdT> {
  private final ErrorReporter myErrorReporter;

  protected ReportingModuleLoader(ErrorReporter myErrorReporter) {
    this.myErrorReporter = myErrorReporter;
  }

  @Override
  public void loadingError(ModuleSourceIdT module, ModuleLoadingError error) {
    myErrorReporter.report(error);
  }
}
