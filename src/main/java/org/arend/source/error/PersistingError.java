package org.arend.source.error;

import org.arend.error.GeneralError;
import org.arend.module.ModulePath;
import org.arend.naming.reference.GlobalReferable;

import java.util.Collection;
import java.util.Collections;

public class PersistingError extends GeneralError {
  public final ModulePath modulePath;

  public PersistingError(ModulePath modulePath) {
    super(Level.INFO, "Module '" + modulePath + "' cannot be persisted");
    this.modulePath = modulePath;
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.emptyList();
  }
}
