package org.arend.ext.prettyprinting.doc;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterConfigImpl;

public class TermLineDoc extends LineDoc {
  private final PrettyPrinterConfig ppConfig;
  private final CoreExpression term;
  private String text;

  TermLineDoc(CoreExpression term, PrettyPrinterConfig ppConfig) {
    this.term = term;
    PrettyPrinterConfigImpl config = new PrettyPrinterConfigImpl(ppConfig);
    config.isSingleLine = true;
    this.ppConfig = config;
  }

  public CoreExpression getTerm() {
    return term;
  }

  public String getText() {
    if (text == null) {
      StringBuilder builder = new StringBuilder();
      term.prettyPrint(builder, ppConfig);
      text = builder.toString();
    }
    return text;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitTermLine(this, params);
  }

  @Override
  public int getWidth() {
    return getText().length();
  }

  @Override
  public boolean isEmpty() {
    return false;
  }
}
