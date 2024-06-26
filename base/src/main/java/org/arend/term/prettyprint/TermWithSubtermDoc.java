package org.arend.term.prettyprint;

import org.arend.core.expr.Expression;
import org.arend.core.subst.Levels;
import org.arend.ext.prettifier.ExpressionPrettifier;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.TermDoc;
import org.arend.ext.reference.Precedence;
import org.arend.naming.renamer.ReferableRenamer;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.FixLevelParameters;

public class TermWithSubtermDoc extends TermDoc {
  private final Expression mySubterm;
  private final Levels myLevels;
  private int myBegin;
  private int myEnd;

  public TermWithSubtermDoc(Expression term, Expression subterm, Levels levels, ExpressionPrettifier prettifier, PrettyPrinterConfig ppConfig) {
    super(term, prettifier, ppConfig);
    this.mySubterm = subterm;
    this.myLevels = levels;
  }

  public Expression getSubterm() {
    return mySubterm;
  }

  public Levels getLevels() {
    return myLevels;
  }

  public int getBegin() {
    return myBegin;
  }

  public int getEnd() {
    return myEnd;
  }

  public void init() {
    getText();
  }

  @Override
  public String getString() {
    StringBuilder builder = new StringBuilder();
    Expression term = (Expression) getTerm();
    PrettyPrinterConfig ppConfig = getPPConfig();
    if (ppConfig.getNormalizationMode() != null) {
      FixLevelParameters.fix(term); // Expressions created in errors might have not fixed levels, so we fix them here
    }
    PrettyPrintWithSubexprVisitor visitor = new PrettyPrintWithSubexprVisitor(builder, 0, !ppConfig.isSingleLine());
    ToAbstractVisitor.convert(term, mySubterm, myLevels, getPrettifier(), ppConfig, new ReferableRenamer()).accept(visitor, new Precedence(Concrete.Expression.PREC));
    String result = builder.toString();
    int firstIndex = result.indexOf(PrettyPrintWithSubexprVisitor.MAGIC);
    int lastIndex = result.indexOf(PrettyPrintWithSubexprVisitor.MAGIC, firstIndex + 1);
    if (lastIndex >= 0) {
      myBegin = firstIndex;
      myEnd = lastIndex - 1;
    }
    if (lastIndex >= 0) {
      result = result.substring(0, firstIndex) + result.substring(firstIndex + 1, lastIndex) + result.substring(lastIndex + 1);
    } else if (firstIndex >= 0) {
      result = result.substring(0, firstIndex) + result.substring(firstIndex + 1);
    }
    return result;
  }
}
