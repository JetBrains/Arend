package org.arend.source.error;

import org.arend.error.GeneralError;
import org.arend.module.ModulePath;

public class PersistingError extends GeneralError {
  public final ModulePath modulePath;

  public PersistingError(ModulePath modulePath) {
    super(Level.INFO, "Module '" + modulePath + "' cannot be persisted");
    this.modulePath = modulePath;
  }
}
