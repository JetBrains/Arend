package org.arend.typechecking.error;

import org.arend.error.ErrorReporter;
import org.arend.typechecking.error.local.LocalError;

public interface LocalErrorReporter extends ErrorReporter {
  void report(LocalError localError);
}
