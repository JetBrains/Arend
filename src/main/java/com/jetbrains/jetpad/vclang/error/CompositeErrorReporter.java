package com.jetbrains.jetpad.vclang.error;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompositeErrorReporter<T> implements ErrorReporter<T> {
  private final List<ErrorReporter<T>> myErrorReporters;

  @SafeVarargs
  public CompositeErrorReporter(ErrorReporter<T>... errorReporters) {
    myErrorReporters = new ArrayList<>(Arrays.asList(errorReporters));
  }

  public void addErrorReporter(ErrorReporter<T> errorReporter) {
    myErrorReporters.add(errorReporter);
  }

  public void removeErrorReporter(ErrorReporter<T> errorReporter) {
    myErrorReporters.remove(errorReporter);
  }

  @Override
  public void report(GeneralError<T> error) {
    for (ErrorReporter<T> errorReporter : myErrorReporters) {
      errorReporter.report(error);
    }
  }
}
