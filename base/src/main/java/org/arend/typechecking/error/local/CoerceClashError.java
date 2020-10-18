package org.arend.typechecking.error.local;

import org.arend.core.definition.Definition;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.jetbrains.annotations.Nullable;

public class CoerceClashError extends TypecheckingError {
  public final Definition definition;

  public CoerceClashError(@Nullable Definition definition, @Nullable ConcreteSourceNode cause) {
    super("", cause);
    this.definition = definition;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    LineDoc doc = DocFactory.text("There is already a \\coerce rule" + (definition == null ? "" : " for definition "));
    return definition == null ? doc : DocFactory.hList(doc, DocFactory.refDoc(definition.getRef()));
  }
}
