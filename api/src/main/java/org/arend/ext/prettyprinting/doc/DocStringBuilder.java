package org.arend.ext.prettyprinting.doc;

import org.arend.ext.module.LongName;

public class DocStringBuilder extends LineDocVisitor {
  private final StringBuilder builder;

  public DocStringBuilder(StringBuilder builder) {
    this.builder = builder;
  }

  public static String build(Doc doc) {
    DocStringBuilder visitor = new DocStringBuilder(new StringBuilder());
    doc.accept(visitor, false);
    return visitor.builder.toString();
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
      builder.append('\n');
    }
    return null;
  }

  @Override
  public Void visitText(TextDoc doc, Boolean newLine) {
    builder.append(doc.getText());
    if (newLine) {
      builder.append('\n');
    }
    return null;
  }

  @Override
  public Void visitTermLine(TermLineDoc doc, Boolean newLine) {
    builder.append(doc.getText());
    if (newLine) {
      builder.append('\n');
    }
    return null;
  }

  @Override
  public Void visitPattern(PatternDoc doc, Boolean newLine) {
    builder.append(doc.getText());
    if (newLine) {
      builder.append('\n');
    }
    return null;
  }

  @Override
  public Void visitReference(ReferenceDoc doc, Boolean newLine) {
    LongName longName = doc.getReference().isClassField() ? null : doc.getReference().getRefLongName();
    builder.append(longName == null || longName.toList().isEmpty() ? doc.getReference().getRefName() : longName.toString());
    if (newLine) {
      builder.append('\n');
    }
    return null;
  }
}
