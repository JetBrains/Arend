package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.List;

public class ListLocalErrorReporter<T> implements LocalErrorReporter<T> {
  private final List<Error<T>> myErrors;

  public ListLocalErrorReporter(List<Error<T>> errors) {
    myErrors = errors;
  }

  @Override
  public void report(GeneralError<T> error) {
    myErrors.add(error);
  }

  @Override
  public void report(LocalTypeCheckingError<T> localError) {
    myErrors.add(localError);
  }
}
