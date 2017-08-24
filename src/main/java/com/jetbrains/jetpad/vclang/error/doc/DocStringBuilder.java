package com.jetbrains.jetpad.vclang.error.doc;

public class DocStringBuilder extends LineDocVisitor {
  private final StringBuilder myBuilder;

  public DocStringBuilder(StringBuilder builder) {
    myBuilder = builder;
  }

  public static String build(Doc doc) {
    DocStringBuilder visitor = new DocStringBuilder(new StringBuilder());
    doc.accept(visitor, false);
    return visitor.myBuilder.toString();
  }

  @Override
  public Void visitHList(HListDoc listDoc, Boolean newLine) {
    for (LineDoc doc : listDoc.getDocs()) {
      doc.accept(this, false);
    }
    if (newLine) {
      myBuilder.append('\n');
    }
    return null;
  }

  @Override
  public Void visitText(TextDoc doc, Boolean newLine) {
    myBuilder.append(doc.getText());
    if (newLine) {
      myBuilder.append('\n');
    }
    return null;
  }

  @Override
  public Void visitTermLine(TermLineDoc doc, Boolean newLine) {
    myBuilder.append(doc.getText());
    if (newLine) {
      myBuilder.append('\n');
    }
    return null;
  }

  @Override
  public Void visitReference(ReferenceDoc doc, Boolean newLine) {
    myBuilder.append(doc.getReference().textRepresentation());
    if (newLine) {
      myBuilder.append('\n');
    }
    return null;
  }
}
