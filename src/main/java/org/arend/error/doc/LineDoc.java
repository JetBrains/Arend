package org.arend.error.doc;

import java.util.Collections;
import java.util.List;

import static org.arend.error.doc.DocFactory.hList;
import static org.arend.error.doc.DocFactory.text;

public abstract class LineDoc extends Doc {
  @Override
  public final int getHeight() {
    return 1;
  }

  @Override
  public final boolean isNull() {
    return false;
  }

  @Override
  public final boolean isSingleLine() {
    return true;
  }

  @Override
  public final List<LineDoc> linearize(int indent, boolean indentFirst) {
    return Collections.singletonList(!indentFirst || indent == 0 ? this : hList(text(HangDoc.getIndent(indent)), this));
  }
}
