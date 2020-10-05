package org.arend.ext.prettyprinting.doc;

import org.arend.ext.core.body.CorePattern;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;

public class PatternDoc extends LineDoc {
  private final PrettyPrinterConfig ppConfig;
  private final CorePattern pattern;
  private String text;

  PatternDoc(CorePattern pattern, PrettyPrinterConfig ppConfig) {
    this.ppConfig = ppConfig;
    this.pattern = pattern;
  }

  public CorePattern getPattern() {
    return pattern;
  }

  public String getText() {
    if (text == null) {
      StringBuilder builder = new StringBuilder();
      pattern.prettyPrint(builder, ppConfig);
      text = builder.toString();
    }
    return text;
  }

  @Override
  public <P, R> R accept(DocVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPattern(this, params);
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
