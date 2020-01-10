package org.arend.library.error;

import org.arend.error.GeneralError;
import org.arend.ext.module.ModulePath;
import org.arend.naming.reference.ModuleReferable;

public class PartialModuleError extends GeneralError {
  public ModulePath modulePath;

  public PartialModuleError(ModulePath modulePath) {
    super(Level.WARNING, "Partial binary source for module '" + modulePath + "' is ignored");
    this.modulePath = modulePath;
  }

  @Override
  public ModuleReferable getCause() {
    return new ModuleReferable(modulePath);
  }
}
