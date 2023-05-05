package org.arend.typechecking.error.local;

import org.arend.core.context.binding.LevelVariable;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.Nullable;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;
import static org.arend.ext.prettyprinting.doc.DocFactory.text;

public class IncorrectLevelTypeError extends TypecheckingError {
  public final Referable referable;
  public final LevelVariable.LvlType expectedType;

  public IncorrectLevelTypeError(Referable referable, LevelVariable.LvlType expectedType, @Nullable ConcreteSourceNode cause) {
    super("", cause);
    this.referable = referable;
    this.expectedType = expectedType;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text("'"), refDoc(referable), text("' should be a " + (expectedType == LevelVariable.LvlType.PLVL ? "p" : "h") + "-variable"));
  }
}
