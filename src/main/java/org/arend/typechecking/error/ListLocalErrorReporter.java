package org.arend.typechecking.error;

import org.arend.error.Error;
import org.arend.error.GeneralError;
import org.arend.typechecking.error.local.LocalError;

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
  public void report(LocalError localError) {
    myErrors.add(localError);
  }
}
