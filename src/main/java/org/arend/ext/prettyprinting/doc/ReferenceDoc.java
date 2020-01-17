package org.arend.ext.prettyprinting.doc;

import org.arend.ext.reference.ArendRef;

public class ReferenceDoc extends LineDoc {
  private final ArendRef myReference;

  ReferenceDoc(ArendRef reference) {
    assert reference != null;
    myReference = reference;
  }

  public ArendRef getReference() {
    return myReference;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitReference(this, params);
  }

  @Override
  public int getWidth() {
    return myReference.getRefName().length();
  }

  @Override
  public boolean isEmpty() {
    return myReference.getRefName().isEmpty();
  }
}
