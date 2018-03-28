package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ModuleReferable implements LocatedReferable {
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

  @Override
  public boolean isModule() {
    return true;
  }

  @Nullable
  @Override
  public ModulePath getLocation() {
    return path;
  }

  @Nullable
  @Override
  public LocatedReferable getLocatedReferableParent() {
    return null;
  }
}
