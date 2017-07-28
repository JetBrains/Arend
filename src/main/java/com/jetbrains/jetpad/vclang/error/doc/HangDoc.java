package com.jetbrains.jetpad.vclang.error.doc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.hList;
import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.text;

public class HangDoc extends Doc {
  private final Doc myTop;
  private final Doc myBottom;

  public final static int INDENT = 2;
  public final static int MAX_INDENT = 6;

  HangDoc(Doc top, Doc bottom) {
    myTop = top;
    myBottom = bottom;
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

  @Override
  public List<LineDoc> linearize() {
    List<LineDoc> result = myTop.linearize();
    if (needNewLine()) {
      for (LineDoc doc : myBottom.linearize()) {
        char[] array = new char[HangDoc.INDENT];
        Arrays.fill(array, ' ');
        result.add(hList(text(Arrays.toString(array)), doc));
      }
    } else {
      assert result.size() <= 1;
      if (result.isEmpty()) {
        result = myBottom.linearize();
      } else {
        List<LineDoc> bottomLines = myBottom.linearize();
        if (bottomLines.size() == 1) {
          result = Collections.singletonList(hList(result.get(0), text(" "), bottomLines.get(0)));
        } else if (bottomLines.size() > 1) {
          int width = result.get(0).getWidth() + 1;
          char[] array = new char[width];
          Arrays.fill(array, ' ');
          LineDoc spaces = text(Arrays.toString(array));
          bottomLines.set(0, hList(result.get(0), text(" "), bottomLines.get(0)));
          for (int i = 1; i < bottomLines.size(); i++) {
            bottomLines.set(i, hList(spaces, bottomLines.get(i)));
          }
          result = bottomLines;
        }
      }
    }
    return result;
  }

  public boolean needNewLine() {
    return !myTop.isSingleLine() && !myBottom.isNull() || !myBottom.isSingleLine() && myTop.getWidth() + (myBottom.isEmpty() ? 0 : 1) > HangDoc.MAX_INDENT;
  }
}
