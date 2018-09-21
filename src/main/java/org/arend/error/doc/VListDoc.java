package org.arend.error.doc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class VListDoc extends Doc {
  private final Collection<? extends Doc> myDocs;

  VListDoc(Collection<? extends Doc> docs) {
    myDocs = docs;
  }

  public Collection<? extends Doc> getDocs() {
    return myDocs;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitVList(this, params);
  }

  @Override
  public int getWidth() {
    int width = 0;
    for (Doc doc : myDocs) {
      width = Math.max(width, doc.getWidth());
    }
    return width;
  }

  @Override
  public int getHeight() {
    int height = 0;
    for (Doc doc : myDocs) {
      height += doc.getHeight();
    }
    return height;
  }

  @Override
  public boolean isNull() {
    for (Doc doc : myDocs) {
      if (!doc.isNull()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isSingleLine() {
    int height = 0;
    for (Doc doc : myDocs) {
      height += doc.getHeight();
      if (height > 1) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isEmpty() {
    for (Doc doc : myDocs) {
      if (!doc.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public List<LineDoc> linearize(int indent, boolean indentFirst) {
    if (myDocs.isEmpty()) {
      return Collections.emptyList();
    }

    List<LineDoc> result = new ArrayList<>();
    for (Doc doc : myDocs) {
      List<LineDoc> docs = doc.linearize(indent, indentFirst);
      if (!docs.isEmpty()) {
        indentFirst = true;
      }
      result.addAll(docs);
    }
    return result;
  }
}
