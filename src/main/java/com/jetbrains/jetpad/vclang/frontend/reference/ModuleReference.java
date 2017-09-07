package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;

public class ModuleReference implements GlobalReferable {
  public final ModulePath path;

  public ModuleReference(ModulePath path) {
    this.path = path;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return path.toString();
  }

  @Override
  public Precedence getPrecedence() {
    return Precedence.DEFAULT;
  }
}
