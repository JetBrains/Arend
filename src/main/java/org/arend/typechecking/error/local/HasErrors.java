package org.arend.typechecking.error.local;

import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;

public class HasErrors extends TypecheckingError {
  public HasErrors(Level level, GlobalReferable definition, Concrete.SourceNode expression) {
    super(level, definition.textRepresentation() + " has errors", expression);
  }
}
