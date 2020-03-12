package org.arend.ext.prettyprinting.doc;

import java.util.Collection;

public class HListDoc extends LineDoc {
  private final Collection<? extends LineDoc> docs;

  HListDoc(Collection<? extends LineDoc> docs) {
    this.docs = docs;
  }

  public Collection<? extends LineDoc> getDocs() {
    return docs;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitHList(this, params);
  }

  @Override
  public int getWidth() {
    int width = 0;
    for (LineDoc doc : docs) {
      width += doc.getWidth();
    }
    return width;
  }

  @Override
  public boolean isEmpty() {
    for (LineDoc doc : docs) {
      if (!doc.isEmpty()) {
        return false;
      }
    }
    return true;
  }
}
