package org.arend.naming.reference;

import org.arend.term.Precedence;

import javax.annotation.Nullable;

public class DataLocatedReferableImpl extends LocatedReferableImpl {
  private ClassReferable myTypeClassReference;

  public DataLocatedReferableImpl(Precedence precedence, String name, LocatedReferable parent, ClassReferable typeClassReference, Kind kind) {
    super(precedence, name, parent, kind);
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
