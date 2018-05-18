package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nullable;

public class DataLocatedReferableImpl extends LocatedReferableImpl {
  private ClassReferable myTypeClassReference;

  public DataLocatedReferableImpl(Precedence precedence, String name, LocatedReferable parent, ClassReferable typeClassReference, boolean isTypecheckable) {
    super(precedence, name, parent, isTypecheckable);
    myTypeClassReference = typeClassReference;
  }

  public DataLocatedReferableImpl(Precedence precedence, String name, ModulePath parent, ClassReferable typeClassReference) {
    super(precedence, name, parent);
    myTypeClassReference = typeClassReference;
  }

  @Nullable
  @Override
  public ClassReferable getTypeClassReference() {
    return myTypeClassReference;
  }

  public void setTypeClassReference(ClassReferable reference) {
    myTypeClassReference = reference;
  }
}
