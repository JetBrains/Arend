package com.jetbrains.jetpad.vclang.error;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompositeErrorReporter implements ErrorReporter {
  private final List<ErrorReporter> myErrorReporters;

  public CompositeErrorReporter(ErrorReporter... errorReporters) {
    myErrorReporters = new ArrayList<>(Arrays.asList(errorReporters));
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
