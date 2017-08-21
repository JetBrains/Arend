package com.jetbrains.jetpad.vclang.error.doc;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

public class ReferenceDoc extends LineDoc {
  private final Referable myReference;

  ReferenceDoc(Referable reference) {
    myReference = reference;
  }

  public Referable getReference() {
    return myReference;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitReference(this, params);
  }

  @Override
  public int getWidth() {
    return myReference.toString().length();
  }

  @Override
  public boolean isEmpty() {
    return myReference.toString().isEmpty();
  }
}
