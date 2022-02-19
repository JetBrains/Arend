package org.arend.typechecking.error.local;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.jetbrains.annotations.Nullable;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class IncorrectImplementationError extends TypecheckingError {
  private final Definition definition;
  private final ClassDefinition classDef;

  public IncorrectImplementationError(ClassField field, ClassDefinition classDef, @Nullable ConcreteSourceNode cause) {
    super("", cause);
    this.definition = field;
    this.classDef = classDef;
  }

  public IncorrectImplementationError(ClassDefinition superClass, ClassDefinition classDef, @Nullable ConcreteSourceNode cause) {
    super("", cause);
    this.definition = superClass;
    this.classDef = classDef;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text(definition instanceof ClassField ? "Field '" : "Class '"), refDoc(definition.getRef()), text(definition instanceof ClassField ? "' does not belong to class '" : "' is not a super class of '"), refDoc(classDef.getRef()), text("'"));
  }
}
