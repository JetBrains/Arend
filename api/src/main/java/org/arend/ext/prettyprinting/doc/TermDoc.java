package org.arend.ext.prettyprinting.doc;

import org.arend.ext.core.expr.UncheckedExpression;
import org.arend.ext.prettifier.ExpressionPrettifier;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.jetbrains.annotations.Nullable;

import static org.arend.ext.prettyprinting.doc.DocFactory.hList;
import static org.arend.ext.prettyprinting.doc.DocFactory.text;

public class TermDoc extends CachingDoc {
  private final UncheckedExpression term;
  private final ExpressionPrettifier prettifier;
  private final PrettyPrinterConfig ppConfig;

  protected TermDoc(UncheckedExpression term, ExpressionPrettifier prettifier, PrettyPrinterConfig ppConfig) {
    this.term = term;
    this.prettifier = prettifier;
    this.ppConfig = ppConfig;
  }

  public UncheckedExpression getTerm() {
    return term;
  }

  public ExpressionPrettifier getPrettifier() {
    return prettifier;
  }

  public PrettyPrinterConfig getPPConfig() {
    return ppConfig;
  }

  @Override
  public String getString() {
    StringBuilder builder = new StringBuilder();
    term.prettyPrint(builder, prettifier, ppConfig);
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
    TermTextDoc termDoc = new TermTextDoc(text, term, isFirst);
    return indent == null ? termDoc : hList(text(indent), termDoc);
  }
}
