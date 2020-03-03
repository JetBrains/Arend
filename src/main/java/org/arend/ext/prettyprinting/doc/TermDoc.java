package org.arend.ext.prettyprinting.doc;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.jetbrains.annotations.Nullable;

import static org.arend.ext.prettyprinting.doc.DocFactory.hList;
import static org.arend.ext.prettyprinting.doc.DocFactory.text;

public class TermDoc extends CachingDoc {
  private final CoreExpression term;
  private final PrettyPrinterConfig ppConfig;

  TermDoc(CoreExpression term, PrettyPrinterConfig ppConfig) {
    this.term = term;
    this.ppConfig = ppConfig;
  }

  public CoreExpression getTerm() {
    return term;
  }

  @Override
  public String getString() {
    StringBuilder builder = new StringBuilder();
    term.prettyPrint(builder, ppConfig);
    return builder.toString();
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTerm(this, params);
  }

  @Override
  protected LineDoc getLineDoc(@Nullable String indent, String text, boolean isFirst) {
    if (isFirst && text.isEmpty()) {
      return null;
    }
    TermTextDoc termDoc = new TermTextDoc(text, isFirst);
    return indent == null ? termDoc : hList(text(indent), termDoc);
  }
}
