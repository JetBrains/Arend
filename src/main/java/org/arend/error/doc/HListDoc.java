package org.arend.error.doc;

import java.util.Collection;

public class HListDoc extends LineDoc {
  private final Collection<? extends LineDoc> myDocs;

  HListDoc(Collection<? extends LineDoc> docs) {
    myDocs = docs;
  }

  public Collection<? extends LineDoc> getDocs() {
    return myDocs;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitHList(this, params);
  }

  @Override
  public int getWidth() {
    int width = 0;
    for (LineDoc doc : myDocs) {
      width += doc.getWidth();
    }
    return width;
  }

  @Override
  public boolean isEmpty() {
    for (LineDoc doc : myDocs) {
      if (!doc.isEmpty()) {
        return false;
      }
    }
    return true;
  }
}
