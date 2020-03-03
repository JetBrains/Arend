package org.arend.ext.prettyprinting.doc;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class TermLineDoc extends LineDoc {
  private final PrettyPrinterConfig ppConfig;
  private final CoreExpression term;
  private String text;

  TermLineDoc(CoreExpression term, PrettyPrinterConfig ppConfig) {
    this.term = term;
    this.ppConfig = new PrettyPrinterConfig() {
        @Override
        public boolean isSingleLine() {
          return true;
        }

        @NotNull
        @Override
        public EnumSet<PrettyPrinterFlag> getExpressionFlags() {
          return ppConfig.getExpressionFlags();
        }

        @Override
        public NormalizationMode getNormalizationMode() {
          return ppConfig.getNormalizationMode();
        }
    };
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
