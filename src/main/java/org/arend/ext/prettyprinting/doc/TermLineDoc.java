package org.arend.ext.prettyprinting.doc;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;

import javax.annotation.Nonnull;
import java.util.EnumSet;

public class TermLineDoc extends LineDoc {
  private final PrettyPrinterConfig myPPConfig;
  private final CoreExpression myTerm;
  private String myText;

  TermLineDoc(CoreExpression term, PrettyPrinterConfig ppConfig) {
    myTerm = term;
    myPPConfig = new PrettyPrinterConfig() {
        @Override
        public boolean isSingleLine() {
          return true;
        }

        @Nonnull
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
    return myTerm;
  }

  public String getText() {
    if (myText == null) {
      StringBuilder builder = new StringBuilder();
      myTerm.prettyPrint(builder, myPPConfig);
      myText = builder.toString();
    }
    return myText;
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
