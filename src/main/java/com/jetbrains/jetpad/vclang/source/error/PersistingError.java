package com.jetbrains.jetpad.vclang.source.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;

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
    return Collections.singletonList(new ModuleReferable(modulePath));
  }
}
