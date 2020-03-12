package org.arend.typechecking.doubleChecker;

import org.arend.ext.error.GeneralError;

public class CoreException extends RuntimeException {
  public final GeneralError error;

  public CoreException(GeneralError error) {
    this.error = error;
  }
}
