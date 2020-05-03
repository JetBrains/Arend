package org.arend.typechecking.error.local;

import org.arend.core.definition.Constructor;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;
import static org.arend.ext.prettyprinting.doc.DocFactory.text;

public class HigherConstructorMatchingError extends TypecheckingError {
  public final Constructor constructor;

  public HigherConstructorMatchingError(Constructor constructor, ConcreteSourceNode cause) {
    super("", cause);
    this.constructor = constructor;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(
      text("Constructor '"),
      refDoc(constructor.getReferable()),
      text("' should be matched since it evaluates to a constructor which is matched"));
  }
}
