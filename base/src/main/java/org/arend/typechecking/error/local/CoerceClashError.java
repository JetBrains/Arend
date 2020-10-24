package org.arend.typechecking.error.local;

import org.arend.core.definition.CoerceData;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.jetbrains.annotations.Nullable;

public class CoerceClashError extends TypecheckingError {
  public final CoerceData.Key key;

  public CoerceClashError(@Nullable CoerceData.Key key, @Nullable ConcreteSourceNode cause) {
    super("", cause);
    this.key = key;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    String s = key instanceof CoerceData.DefinitionKey ? " for definition "
      : key instanceof CoerceData.PiKey ? " for \\Pi-types"
      : key instanceof CoerceData.SigmaKey ? " for \\Sigma-types"
      : key instanceof CoerceData.UniverseKey ? " for universes"
      : "";
    LineDoc doc = DocFactory.text("There is already a \\coerce rule" + s);
    return key instanceof CoerceData.DefinitionKey ? DocFactory.hList(doc, DocFactory.refDoc(((CoerceData.DefinitionKey) key).definition.getRef())) : doc;
  }
}
