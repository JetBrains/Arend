package com.jetbrains.jetpad.vclang.error;

import java.util.ArrayList;
import java.util.List;

public class CompositeErrorReporter implements ErrorReporter {
  private final List<ErrorReporter> myErrorReporters;

  public CompositeErrorReporter() {
    myErrorReporters = new ArrayList<>(2);
  }

  public CompositeErrorReporter(List<ErrorReporter> errorReporters) {
    myErrorReporters = errorReporters;
  }

  public void addErrorReporter(ErrorReporter errorReporter) {
    myErrorReporters.add(errorReporter);
  }

  public void removeErrorReporter(ErrorReporter errorReporter) {
    myErrorReporters.remove(errorReporter);
  }

  @Override
  public void report(GeneralError error) {
    for (ErrorReporter errorReporter : myErrorReporters) {
      errorReporter.report(error);
    }
  }
}
