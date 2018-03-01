package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class LocatedReferableImpl implements LocatedReferable {
  private final Precedence myPrecedence;
  private final String myName;
  private final Object myParent;
  private final boolean myTypecheckable;

  public LocatedReferableImpl(Precedence precedence, String name, LocatedReferable parent, boolean isTypecheckable) {
    myPrecedence = precedence;
    myName = name;
    myParent = parent;
    myTypecheckable = isTypecheckable;
  }

  public LocatedReferableImpl(Precedence precedence, String name, ModulePath parent) {
    myPrecedence = precedence;
    myName = name;
    myParent = parent;
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
  public GlobalReferable getTypecheckable() {
    return myTypecheckable ? this : (GlobalReferable) myParent;
  }

  @Nullable
  @Override
  public ModulePath getLocation(List<? super String> fullName) {
    ModulePath modulePath;
    if (myParent instanceof LocatedReferable) {
      modulePath = ((LocatedReferable) myParent).getLocation(fullName);
    } else {
      modulePath = (ModulePath) myParent;
    }
    fullName.add(myName);
    return modulePath;
  }

  @Override
  public String toString() {
    return myName;
  }
}
