package org.arend.typechecking.error;

import org.arend.error.ErrorReporter;
import org.arend.error.GeneralError;

import java.util.List;

public class ListErrorReporter implements ErrorReporter {
  private final List<GeneralError> myErrors;

  public ListErrorReporter(List<GeneralError> errors) {
    myErrors = errors;
  }

  @Override
  public void report(GeneralError error) {
    myErrors.add(error);
  }
}
