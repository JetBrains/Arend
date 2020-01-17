package org.arend.ext.prettyprinting.doc;

import org.arend.ext.module.LongName;

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

  public static void build(StringBuilder builder, Doc doc) {
    doc.accept(new DocStringBuilder(builder), false);
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
    LongName longName = doc.getReference().isClassField() ? null : doc.getReference().getRefLongName();
    myBuilder.append(longName == null ? doc.getReference().getRefName() : longName.toString());
    if (newLine) {
      myBuilder.append('\n');
    }
    return null;
  }
}
