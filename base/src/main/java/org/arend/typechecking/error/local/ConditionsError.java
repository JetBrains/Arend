package org.arend.typechecking.error.local;

import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.patternmatching.Condition;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ConditionsError extends TypecheckingError {
  public final Condition condition1;
  public final Condition condition2;

  public ConditionsError(Condition condition1, Condition condition2, Concrete.SourceNode sourceNode) {
    super("Conditions check failed", sourceNode);
    this.condition1 = condition1;
    this.condition2 = condition2;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      condition1.toDoc(ppConfig),
      text("while the right hand side"),
      condition2.toDoc(ppConfig));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
