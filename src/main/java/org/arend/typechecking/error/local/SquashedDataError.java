package org.arend.typechecking.error.local;

import org.arend.core.definition.DataDefinition;
import org.arend.error.doc.LineDoc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

import static org.arend.error.doc.DocFactory.*;

public class SquashedDataError extends TypecheckingError {
  public final DataDefinition dataDef;
  public final org.arend.core.sort.Level level;

  public SquashedDataError(DataDefinition dataDef, org.arend.core.sort.Level level, @Nonnull Concrete.SourceNode cause) {
    super("", cause);
    this.dataDef = dataDef;
    this.level = level;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("Pattern matching on " + (dataDef.isTruncated() ? "truncated" : "squashed") + " data type '"), refDoc(dataDef.getReferable()), text("' is allowed only in \\sfunc and \\scase"));
  }
}
