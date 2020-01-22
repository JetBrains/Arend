package org.arend.ext.prettyprinting.doc;

import org.arend.ext.reference.ArendRef;

public class ReferenceDoc extends LineDoc {
  private final ArendRef reference;

  ReferenceDoc(ArendRef reference) {
    assert reference != null;
    this.reference = reference;
  }

  public ArendRef getReference() {
    return reference;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitReference(this, params);
  }

  @Override
  public int getWidth() {
    return reference.getRefName().length();
  }

  @Override
  public boolean isEmpty() {
    return reference.getRefName().isEmpty();
  }
}
