package org.arend.ext.prettyprinting.doc;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.hList;
import static org.arend.ext.prettyprinting.doc.DocFactory.text;

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

  @NotNull
  @Override
  public final List<LineDoc> linearize(int indent, boolean indentFirst) {
    return Collections.singletonList(!indentFirst || indent == 0 ? this : hList(text(HangDoc.getIndent(indent)), this));
  }
}
