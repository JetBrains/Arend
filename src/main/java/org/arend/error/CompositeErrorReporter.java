package org.arend.error;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompositeErrorReporter implements ErrorReporter {
  private final List<ErrorReporter> myErrorReporters;

  public CompositeErrorReporter(ErrorReporter... errorReporters) {
    myErrorReporters = new ArrayList<>(Arrays.asList(errorReporters));
  }

  public List<ErrorReporter> getErrorReporters() {
    return myErrorReporters;
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
