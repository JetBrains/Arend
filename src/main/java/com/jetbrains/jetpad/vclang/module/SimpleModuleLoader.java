package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.ListErrorReporter;

public class SimpleModuleLoader extends BaseModuleLoader {
  private final ListErrorReporter myErrorReporter;

  public SimpleModuleLoader(boolean recompile) {
    super(recompile);
    myErrorReporter = new ListErrorReporter();
  }

  public SimpleModuleLoader(ListErrorReporter errorReporter, boolean recompile) {
    super(recompile);
    myErrorReporter = errorReporter;
  }

  public ListErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  @Override
  public void loadingError(GeneralError error) {
    myErrorReporter.report(error);
  }

  @Override
  public void loadingSucceeded(Namespace namespace, ClassDefinition classDefinition, boolean compiled) {

  }
}
