package com.jetbrains.jetpad.vclang.library.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;

import java.util.Collection;
import java.util.Collections;

public class PartialModuleError extends GeneralError {
  public ModulePath modulePath;

  public PartialModuleError(ModulePath modulePath) {
    super(Level.WARNING, "Partial binary source for module '" + modulePath + "' is ignored");
    this.modulePath = modulePath;
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.singletonList(new ModuleReferable(modulePath));
  }
}
