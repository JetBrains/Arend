package com.jetbrains.jetpad.vclang.error.doc;

import java.util.List;

public abstract class LineDocVisitor implements DocVisitor<Boolean, Void> {
  public void visitDoc(Doc doc, Boolean newLine) {
    List<LineDoc> lineDocs = doc.linearize();
    for (int i = 0; i < lineDocs.size(); i++) {
      lineDocs.get(i).accept(this, i == lineDocs.size() - 1 ? newLine : true);
    }
  }

  @Override
  public Void visitVList(VListDoc doc, Boolean newLine) {
    visitDoc(doc, newLine);
    return null;
  }

  @Override
  public Void visitHang(HangDoc doc, Boolean newLine) {
    visitDoc(doc, newLine);
    return null;
  }

  @Override
  public Void visitCaching(CachingDoc doc, Boolean newLine) {
    visitDoc(doc, newLine);
    return null;
  }
}
