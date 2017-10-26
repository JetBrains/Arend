package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;

public class ModuleReferable implements GlobalReferable {
  public final ModulePath path;

  public ModuleReferable(ModulePath path) {
    this.path = path;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return path.toString();
  }

  @Nonnull
  @Override
  public Precedence getPrecedence() {
    return Precedence.DEFAULT;
  }
}
