package org.arend.typechecking.error.local;

import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.term.concrete.Concrete;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;
import static org.arend.ext.prettyprinting.doc.DocFactory.text;

public class LevelFieldsDifferError extends TypecheckingError {
  public final Concrete.ReferenceExpression firstSuperClass;

  public LevelFieldsDifferError(Concrete.ReferenceExpression superClass1, Concrete.ReferenceExpression superClass2) {
    super("", superClass2);
    firstSuperClass = superClass1;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("The level parameters of the super class do not match the level parameters of the first super class '"), refDoc(firstSuperClass.getReferent()), text("'"));
  }
}
