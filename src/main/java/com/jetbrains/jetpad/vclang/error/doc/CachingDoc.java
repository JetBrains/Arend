package com.jetbrains.jetpad.vclang.error.doc;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CachingDoc extends Doc {
  private List<String> myText;

  protected abstract String getString();

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
  public boolean isNull() {
    return getText().isEmpty();
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
  public List<LineDoc> linearize() {
    return getText().stream().map(DocFactory::text).collect(Collectors.toList());
  }
}
