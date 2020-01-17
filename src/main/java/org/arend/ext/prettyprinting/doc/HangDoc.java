package org.arend.ext.prettyprinting.doc;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.hList;
import static org.arend.ext.prettyprinting.doc.DocFactory.text;

public class HangDoc extends Doc {
  private final Doc myTop;
  private final Doc myBottom;

  public final static int INDENT = 2;
  public final static int MAX_INDENT = 6;

  HangDoc(Doc top, Doc bottom) {
    myTop = top;
    myBottom = bottom;
  }

  public static String getIndent(int indent) {
    char[] array = new char[indent];
    Arrays.fill(array, ' ');
    return String.valueOf(array);
  }

  public Doc getTop() {
    return myTop;
  }

  public Doc getBottom() {
    return myBottom;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitHang(this, params);
  }

  @Override
  public int getWidth() {
    if (myBottom.isNull()) {
      return myTop.getWidth();
    }
    if (myTop.isEmpty()) {
      return myBottom.getWidth() + INDENT;
    }

    if (needNewLine()) {
      return Math.max(myTop.getWidth(), myBottom.getWidth() + INDENT);
    } else {
      int bottomWidth = myBottom.getWidth();
      return myTop.getWidth() + (bottomWidth == 0 ? 0 : 1) + bottomWidth;
    }
  }

  @Override
  public int getHeight() {
    return myBottom.isNull() ? myTop.getHeight() : needNewLine() ? myTop.getHeight() + myBottom.getHeight() : myBottom.getHeight();
  }

  @Override
  public boolean isNull() {
    return myTop.isNull() && myBottom.isNull();
  }

  @Override
  public boolean isSingleLine() {
    return myTop.isSingleLine() && myBottom.isSingleLine();
  }

  @Override
  public boolean isEmpty() {
    return myBottom.isNull() && myTop.isEmpty();
  }

  @Nonnull
  @Override
  public List<LineDoc> linearize(int indent, boolean indentFirst) {
    List<LineDoc> result = myTop.linearize(indent, indentFirst);
    if (result.isEmpty()) {
      return myBottom.linearize(indent + INDENT, indentFirst);
    }
    if (myBottom.isNull()) {
      return result;
    }

    LineDoc lineDoc = result.get(0);
    if (result.size() == 1 && (myBottom.isSingleLine() || lineDoc.getWidth() + (myBottom.isEmpty() ? 0 : 1) <= MAX_INDENT)) {
      result = myBottom.linearize(lineDoc.getWidth() + 1, false);
      lineDoc = hList(lineDoc, text(" "), result.get(0));
      if (result.size() == 1) {
        result = Collections.singletonList(lineDoc);
      } else {
        result.set(0, lineDoc);
      }
    } else {
      List<LineDoc> bottomLines = myBottom.linearize(indent + INDENT, true);
      if (result.size() == 1) {
        result = new ArrayList<>(bottomLines.size() + 1);
        result.add(lineDoc);
      }
      result.addAll(bottomLines);
    }
    return result;
  }

  public boolean needNewLine() {
    return !myTop.isSingleLine() && !myBottom.isNull() || !myBottom.isSingleLine() && myTop.getWidth() + (myBottom.isEmpty() ? 0 : 1) > MAX_INDENT;
  }
}
