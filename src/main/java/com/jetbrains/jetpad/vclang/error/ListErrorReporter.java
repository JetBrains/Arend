package com.jetbrains.jetpad.vclang.error;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListErrorReporter<T> implements ErrorReporter<T> {
  final private List<GeneralError<T>> myErrorList;

  public ListErrorReporter() {
    myErrorList = new ArrayList<>();
  }

  public ListErrorReporter(List<GeneralError<T>> errorList) {
    myErrorList = errorList;
  }

  @Override
  public void report(GeneralError<T> error) {
    myErrorList.add(error);
  }

  public Collection<? extends GeneralError<T>> getErrorList() {
    return myErrorList;
  }

  public void reportTo(ErrorReporter<T> errorReporter) {
    for (GeneralError<T> error : myErrorList) {
      errorReporter.report(error);
    }
  }
}
