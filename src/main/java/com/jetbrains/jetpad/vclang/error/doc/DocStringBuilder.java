package com.jetbrains.jetpad.vclang.error.doc;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Arrays;

public class DocStringBuilder implements DocVisitor<Integer, Void> {
  private final StringBuilder myBuilder;

  private DocStringBuilder() {
    myBuilder = new StringBuilder();
  }

  public static String build(Doc doc) {
    DocStringBuilder visitor = new DocStringBuilder();
    doc.accept(visitor, 0);
    return visitor.myBuilder.toString();
  }

  private void printIndent(int indent) {
    if (indent > 0) {
      char[] array = new char[indent];
      Arrays.fill(array, ' ');
      myBuilder.append(array);
    }
  }

  @Override
  public Void visitVList(VListDoc listDoc, Integer indent) {
    boolean first = true;
    for (Doc doc : listDoc.getDocs()) {
      if (!doc.isNull()) {
        if (first) {
          first = false;
        } else {
          myBuilder.append('\n');
          printIndent(indent);
        }
        doc.accept(this, indent);
      }
    }
    return null;
  }

  @Override
  public Void visitHList(HListDoc listDoc, Integer indent) {
    for (LineDoc doc : listDoc.getDocs()) {
      doc.accept(this, indent);
    }
    return null;
  }

  @Override
  public Void visitText(TextDoc doc, Integer indent) {
    myBuilder.append(doc.getText());
    return null;
  }

  @Override
  public Void visitHang(HangDoc doc, Integer indent) {
    if (doc.getBottom().isNull()) {
      doc.getTop().accept(this, indent);
    } else if (doc.getTop().isEmpty() && doc.getTop().isSingleLine()) {
      printIndent(HangDoc.INDENT);
      doc.getBottom().accept(this, indent + HangDoc.INDENT);
    } else {
      doc.getTop().accept(this, indent);
      if (doc.needNewLine()) {
        myBuilder.append('\n');
        printIndent(indent + HangDoc.INDENT);
        doc.getBottom().accept(this, indent + HangDoc.INDENT);
      } else {
        boolean bottomIsEmpty = doc.getBottom().isEmpty();
        if (!bottomIsEmpty) {
          myBuilder.append(' ');
        }
        doc.getBottom().accept(this, indent + doc.getTop().getWidth() + (bottomIsEmpty ? 0 : 1));
      }
    }
    return null;
  }

  @Override
  public Void visitCaching(CachingDoc doc, Integer indent) {
    boolean first = true;
    for (String line : doc.getText()) {
      if (first) {
        first = false;
      } else {
        myBuilder.append('\n');
        printIndent(indent);
      }
      myBuilder.append(line);
    }
    return null;
  }

  @Override
  public Void visitTermLine(TermLineDoc doc, Integer params) {
    myBuilder.append(doc.getText());
    return null;
  }

  @Override
  public Void visitReference(ReferenceDoc doc, Integer indent) {
    myBuilder.append(doc.getReference() instanceof Abstract.ReferableSourceNode ? ((Abstract.ReferableSourceNode) doc.getReference()).getName() : doc.getReference());
    return null;
  }
}
