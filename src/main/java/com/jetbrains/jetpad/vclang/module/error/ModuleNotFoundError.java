package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.ModuleReferable;

import java.util.Collection;
import java.util.Collections;

public class ModuleNotFoundError extends GeneralError {
  public final ModulePath modulePath;

  public ModuleNotFoundError(ModulePath modulePath) {
    super(Level.ERROR, "Module not found: " + modulePath);
    this.modulePath = modulePath;
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.singletonList(new ModuleReferable(modulePath));
  }
}
