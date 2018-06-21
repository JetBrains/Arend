package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LocatedReferableImpl implements TCReferable {
  private final Precedence myPrecedence;
  private final String myName;
  private final LocatedReferable myParent;
  private final boolean myTypecheckable;

  public LocatedReferableImpl(Precedence precedence, String name, LocatedReferable parent, boolean isTypecheckable) {
    assert isTypecheckable || parent instanceof TCReferable;
    myPrecedence = precedence;
    myName = name;
    myParent = parent;
    myTypecheckable = isTypecheckable;
  }

  public LocatedReferableImpl(Precedence precedence, String name, ModulePath parent) {
    myPrecedence = precedence;
    myName = name;
    myParent = new ModuleReferable(parent);
    myTypecheckable = true;
  }

  @Nonnull
  @Override
  public Precedence getPrecedence() {
    return myPrecedence;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return myName;
  }

  @Override
  public TCReferable getTypecheckable() {
    return myTypecheckable ? this : (TCReferable) myParent;
  }

  @Override
  public boolean isTypecheckable() {
    return myTypecheckable;
  }

  @Nullable
  @Override
  public ModulePath getLocation() {
    return myParent instanceof ModuleReferable ? ((ModuleReferable) myParent).path : myParent == null ? null : myParent.getLocation();
  }

  @Nullable
  @Override
  public LocatedReferable getLocatedReferableParent() {
    return myParent;
  }

  @Nullable
  @Override
  public LocatedReferable getUnderlyingReference() {
    return null;
  }

  @Override
  public String toString() {
    return myName;
  }
}
