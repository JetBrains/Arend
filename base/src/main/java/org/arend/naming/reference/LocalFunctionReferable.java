package org.arend.naming.reference;

import org.arend.core.definition.Definition;
import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalFunctionReferable implements TCDefReferable {
  private final String myName;
  private final Precedence myPrecedence;
  private final LocatedReferable myParent;
  private Definition myTypechecked;
  private Concrete.Definition myConcrete;

  public LocalFunctionReferable(String name, Precedence precedence, LocatedReferable parent) {
    myName = name;
    myPrecedence = precedence;
    myParent = parent;
  }

  @NotNull
  @Override
  public Precedence getPrecedence() {
    return myPrecedence;
  }

  @NotNull
  @Override
  public String textRepresentation() {
    return myName;
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
    return Kind.FUNCTION;
  }

  @Override
  public Concrete.Definition getDefaultConcrete() {
    return myConcrete;
  }

  public void setConcrete(Concrete.Definition concrete) {
    myConcrete = concrete;
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

  @Override
  public boolean isLocal() {
    return true;
  }
}
