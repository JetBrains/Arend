package org.arend.naming.reference;

import org.arend.ext.module.ModulePath;
import org.arend.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LocatedReferableImpl implements TCReferable {
  private final Precedence myPrecedence;
  private final String myName;
  private final LocatedReferable myParent;
  private final Kind myKind;

  public LocatedReferableImpl(Precedence precedence, String name, @Nullable LocatedReferable parent, Kind kind) {
    assert kind.isTypecheckable() || parent instanceof TCReferable;
    myPrecedence = precedence;
    myName = name;
    myParent = parent;
    myKind = kind;
  }

  public LocatedReferableImpl(Precedence precedence, String name, @Nonnull ModulePath parent, Kind kind) {
    myPrecedence = precedence;
    myName = name;
    myParent = new ModuleReferable(parent);
    myKind = kind;
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
    return myKind.isTypecheckable() ? this : (TCReferable) myParent;
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return myKind;
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

  @Override
  public String toString() {
    return myName;
  }

  @Nullable
  @Override
  public Object getData() {
    return null;
  }
}
