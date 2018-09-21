package org.arend.error.doc;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.ToAbstractVisitor;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.EnumSet;

public class TermLineDoc extends LineDoc {
  private final PrettyPrinterConfig myPPConfig;
  private final Expression myTerm;
  private String myText;

  TermLineDoc(Expression term, PrettyPrinterConfig ppConfig) {
    myTerm = term;
    myPPConfig = new PrettyPrinterConfig() {
        @Override
        public boolean isSingleLine() {
          return true;
        }

        @Override
        public EnumSet<ToAbstractVisitor.Flag> getExpressionFlags() {
          return ppConfig.getExpressionFlags();
        }

        @Override
        public NormalizeVisitor.Mode getNormalizationMode() {
          return ppConfig.getNormalizationMode();
        }
    };
  }

  public Expression getTerm() {
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
