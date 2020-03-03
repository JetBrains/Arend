package org.arend.ext.prettyprinting.doc;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class VListDoc extends Doc {
  private final Collection<? extends Doc> docs;

  VListDoc(Collection<? extends Doc> docs) {
    this.docs = docs;
  }

  public Collection<? extends Doc> getDocs() {
    return docs;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitVList(this, params);
  }

  @Override
  public int getWidth() {
    int width = 0;
    for (Doc doc : docs) {
      width = Math.max(width, doc.getWidth());
    }
    return width;
  }

  @Override
  public int getHeight() {
    int height = 0;
    for (Doc doc : docs) {
      height += doc.getHeight();
    }
    return height;
  }

  @Override
  public boolean isNull() {
    for (Doc doc : docs) {
      if (!doc.isNull()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isSingleLine() {
    int height = 0;
    for (Doc doc : docs) {
      height += doc.getHeight();
      if (height > 1) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isEmpty() {
    for (Doc doc : docs) {
      if (!doc.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  @Override
  public List<LineDoc> linearize(int indent, boolean indentFirst) {
    if (docs.isEmpty()) {
      return Collections.emptyList();
    }

    List<LineDoc> result = new ArrayList<>();
    for (Doc doc : docs) {
      List<LineDoc> docs = doc.linearize(indent, indentFirst);
      if (!docs.isEmpty()) {
        indentFirst = true;
      }
      result.addAll(docs);
    }
    return result;
  }
}
