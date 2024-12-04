package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.jetbrains.annotations.Nullable;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class NotInDynamicScopeError extends TypecheckingError {
  public final CoreClassDefinition classDef;
  public final String fieldName;

  public NotInDynamicScopeError(CoreClassDefinition classDef, String fieldName, @Nullable ConcreteSourceNode cause) {
    super("", cause);
    this.classDef = classDef;
    this.fieldName = fieldName;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("Cannot find '" + fieldName + "' in class '"), refDoc(classDef.getRef()), text("'"));
  }
}
