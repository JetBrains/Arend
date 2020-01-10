package org.arend.error.doc;

import org.arend.ext.module.LongName;
import org.arend.naming.reference.FieldReferable;
import org.arend.naming.reference.LocatedReferable;

import java.util.ArrayList;
import java.util.List;

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
    String name;
    if (doc.getReference() instanceof LocatedReferable && !(doc.getReference() instanceof FieldReferable)) {
      List<String> fullName = new ArrayList<>();
      LocatedReferable.Helper.getLocation((LocatedReferable) doc.getReference(), fullName);
      name = new LongName(fullName).toString();
    } else {
      name = doc.getReference().textRepresentation();
    }

    myBuilder.append(name);
    if (newLine) {
      myBuilder.append('\n');
    }
    return null;
  }
}
