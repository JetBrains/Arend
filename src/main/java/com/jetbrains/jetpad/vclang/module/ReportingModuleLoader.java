package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;

public class ReportingModuleLoader extends BaseModuleLoader {
  private ErrorReporter myErrorReporter;

  public ReportingModuleLoader(ErrorReporter errorReporter, boolean recompile) {
    super(recompile);
    myErrorReporter = errorReporter;
  }

  public void setErrorReporter(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  @Override
  public void savingError(GeneralError error) {
    myErrorReporter.report(error);
  }

  @Override
  public void loadingError(GeneralError error) {
    myErrorReporter.report(error);
  }

  @Override
  public void loadingSucceeded(ModuleID module, Abstract.ClassDefinition abstractDefinition, ClassDefinition compiledDefinition, boolean compiled) {

  }
}
