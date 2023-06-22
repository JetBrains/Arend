package org.arend.naming.reference;

import org.arend.core.definition.Definition;
import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.arend.term.group.AccessModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocatedReferableImpl implements TCDefReferable {
  private final AccessModifier myAccessModifier;
  private Precedence myPrecedence;
  private final String myName;
  private final LocatedReferable myParent;
  private Kind myKind;
  private Definition myTypechecked;

  public LocatedReferableImpl(AccessModifier accessModifier, Precedence precedence, String name, @Nullable LocatedReferable parent, Kind kind) {
    myAccessModifier = accessModifier;
    myPrecedence = precedence;
    myName = name;
    myParent = parent;
    myKind = kind;
  }

  public boolean isPrecedenceSet() {
    return myPrecedence != null;
  }

  @NotNull
  @Override
  public Precedence getPrecedence() {
    return myPrecedence == null ? Precedence.DEFAULT : myPrecedence;
  }

  public void setPrecedence(Precedence precedence) {
    myPrecedence = precedence;
  }

  @NotNull
  @Override
  public String textRepresentation() {
    return myName;
  }

  @Override
  public @NotNull TCDefReferable getTypecheckable() {
    return myKind.isTypecheckable() || myKind == Kind.OTHER || !(myParent instanceof TCDefReferable) ? this : (TCDefReferable) myParent;
  }

  @Override
  public void setTypechecked(Definition definition) {
    myTypechecked = definition;
  }

  @Override
  public Definition getTypechecked() {
    return myTypechecked;
  }

  @NotNull
  @Override
  public Kind getKind() {
    return myKind;
  }

  public void setKind(Kind kind) {
    myKind = kind;
  }

  @Override
  public @NotNull AccessModifier getAccessModifier() {
    return myAccessModifier;
  }

  @Nullable
  @Override
  public ModuleLocation getLocation() {
    return myParent == null ? null : myParent.getLocation();
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
