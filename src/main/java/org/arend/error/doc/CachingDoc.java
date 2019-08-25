package org.arend.error.doc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CachingDoc extends Doc {
  private List<String> myText;

  public abstract String getString();

  public List<? extends String> getText() {
    if (myText == null) {
      myText = Arrays.asList(getString().split("\\n"));
    }
    return myText;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitCaching(this, params);
  }

  @Override
  public int getWidth() {
    int width = 0;
    for (String line : getText()) {
      width = Math.max(width, line.length());
    }
    return width;
  }

  @Override
  public int getHeight() {
    return getText().size();
  }

  @Override
  public final boolean isNull() {
    return false;
  }

  @Override
  public boolean isSingleLine() {
    return getText().size() <= 1;
  }

  @Override
  public boolean isEmpty() {
    for (String line : getText()) {
      if (!line.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public List<LineDoc> linearize(int indent, boolean indentFirst) {
    List<? extends String> text = getText();
    List<LineDoc> result = new ArrayList<>(text.size());
    for (int i = 0; i < text.size(); i++) {
      result.add(DocFactory.text(indent == 0 || !indentFirst && i == 0 ? text.get(i) : HangDoc.getIndent(indent) + text.get(i)));
    }
    return result;
  }
}
