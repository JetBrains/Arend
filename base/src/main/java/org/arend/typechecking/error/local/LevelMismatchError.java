package org.arend.typechecking.error.local;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Sort;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.jetbrains.annotations.Nullable;

public class LevelMismatchError extends TypecheckingError {
  public final boolean isLemma;
  public final Sort actualSort;

  public LevelMismatchError(boolean isLemma, Sort sort, @Nullable ConcreteSourceNode cause) {
    super("The type of a " + (isLemma ? "lemma" : "property") + " must be a proposition", cause);
    this.isLemma = isLemma;
    actualSort = sort != null ? sort : new Sort(new org.arend.core.sort.Level(LevelVariable.PVAR), org.arend.core.sort.Level.INFINITY);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return DocFactory.hList(DocFactory.text("Actual level: "), DocFactory.termLine(new UniverseExpression(actualSort), ppConfig));
  }
}
