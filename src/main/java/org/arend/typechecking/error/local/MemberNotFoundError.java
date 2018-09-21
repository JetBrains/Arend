package org.arend.typechecking.error.local;

import org.arend.core.definition.Definition;
import org.arend.error.doc.LineDoc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

public class MemberNotFoundError extends TypecheckingError {
  public final Definition targetDefinition;
  public final String name;

  private MemberNotFoundError(Definition targetDefinition, String name, String message, Concrete.SourceNode cause) {
    super(message, cause);
    this.targetDefinition = targetDefinition;
    this.name = name;
  }

  public MemberNotFoundError(Definition targetDefinition, String name, Concrete.SourceNode cause) {
    this(targetDefinition, name, "Member not found", cause);
  }

  public MemberNotFoundError(Definition targetDefinition, String name, boolean isStatic, Concrete.SourceNode cause) {
    this(targetDefinition, name, (isStatic ? "Static" : "Dynamic") + " member not found", cause);
  }

  @Override
  public LineDoc getBodyDoc(PrettyPrinterConfig src) {
    return hList(text(name + " of some compiled definition called "), refDoc(targetDefinition.getReferable()));
  }
}
