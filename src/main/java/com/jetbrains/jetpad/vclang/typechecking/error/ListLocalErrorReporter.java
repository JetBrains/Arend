package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.List;

public class ListLocalErrorReporter implements LocalErrorReporter {
  private final List<Error> myErrors;

  public ListLocalErrorReporter(List<Error> errors) {
    myErrors = errors;
  }

  @Override
  public void report(GeneralError error) {
    myErrors.add(error);
  }

  @Override
  public void report(LocalTypeCheckingError localError) {
    myErrors.add(localError);
  }
}
