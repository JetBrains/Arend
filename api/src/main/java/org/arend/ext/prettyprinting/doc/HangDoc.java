package org.arend.ext.prettyprinting.doc;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.hList;
import static org.arend.ext.prettyprinting.doc.DocFactory.text;

public class HangDoc extends Doc {
  private final Doc top;
  private final Doc bottom;

  public final static int INDENT = 2;
  public final static int MAX_INDENT = 6;

  HangDoc(Doc top, Doc bottom) {
    this.top = top;
    this.bottom = bottom;
  }

  public static String getIndent(int indent) {
    char[] array = new char[indent];
    Arrays.fill(array, ' ');
    return String.valueOf(array);
  }

  public Doc getTop() {
    return top;
  }

  public Doc getBottom() {
    return bottom;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitHang(this, params);
  }

  @Override
  public int getWidth() {
    if (bottom.isNull()) {
      return top.getWidth();
    }
    if (top.isEmpty()) {
      return bottom.getWidth() + INDENT;
    }

    if (needNewLine()) {
      return Math.max(top.getWidth(), bottom.getWidth() + INDENT);
    } else {
      int bottomWidth = bottom.getWidth();
      return top.getWidth() + (bottomWidth == 0 ? 0 : 1) + bottomWidth;
    }
  }

  @Override
  public int getHeight() {
    return bottom.isNull() ? top.getHeight() : needNewLine() ? top.getHeight() + bottom.getHeight() : bottom.getHeight();
  }

  @Override
  public boolean isNull() {
    return top.isNull() && bottom.isNull();
  }

  @Override
  public boolean isSingleLine() {
    return top.isSingleLine() && bottom.isSingleLine();
  }

  @Override
  public boolean isEmpty() {
    return bottom.isNull() && top.isEmpty();
  }

  @NotNull
  @Override
  public List<LineDoc> linearize(int indent, boolean indentFirst) {
    List<LineDoc> result = top.linearize(indent, indentFirst);
    if (result.isEmpty()) {
      return bottom.linearize(indent + INDENT, indentFirst);
    }
    if (bottom.isNull()) {
      return result;
    }

    LineDoc lineDoc = result.get(0);
    if (result.size() == 1 && (bottom.isSingleLine() || lineDoc.getWidth() + (bottom.isEmpty() ? 0 : 1) <= MAX_INDENT)) {
      result = bottom.linearize(lineDoc.getWidth() + 1, false);
      lineDoc = hList(lineDoc, text(" "), result.get(0));
      if (result.size() == 1) {
        result = Collections.singletonList(lineDoc);
      } else {
        result.set(0, lineDoc);
      }
    } else {
      List<LineDoc> bottomLines = bottom.linearize(indent + INDENT, true);
      if (result.size() == 1) {
        result = new ArrayList<>(bottomLines.size() + 1);
        result.add(lineDoc);
      }
      result.addAll(bottomLines);
    }
    return result;
  }

  public boolean needNewLine() {
    return !top.isSingleLine() && !bottom.isNull() || !bottom.isSingleLine() && top.getWidth() + (bottom.isEmpty() ? 0 : 1) > MAX_INDENT;
  }
}
