package org.arend.naming.error;

import org.arend.error.SourceInfo;
import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.naming.reference.Referable;
import org.arend.term.prettyprint.PrettyPrinterConfig;

public class DuplicateNameError extends ReferenceError {
  private final Referable previous;

  public DuplicateNameError(Level level, Referable referable, Referable previous) {
    super(level, Stage.RESOLVER, "Duplicate name: " + referable.textRepresentation(), referable);
    this.previous = previous;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    return getBodyDoc(referable, previous);
  }

  static Doc getBodyDoc(Referable referable, Referable previous) {
    String text = "Previous occurrence";
    if (!(previous instanceof SourceInfo)) {
      return DocFactory.hList(DocFactory.text(text + ": "), DocFactory.refDoc(previous));
    }

    String module = ((SourceInfo) previous).moduleTextRepresentation();
    if (module != null) {
      String module1 = referable instanceof SourceInfo ? ((SourceInfo) referable).moduleTextRepresentation() : null;
      if (module.equals(module1)) {
        module = null;
      }
    }

    String position = ((SourceInfo) previous).positionTextRepresentation();
    if (module == null && position == null) {
      return DocFactory.text(text);
    }

    if (module == null) {
      module = "";
    }
    if (position == null) {
      position = "";
    }
    return DocFactory.text(text + " at " + module + (module.isEmpty() || position.isEmpty() ? "" : ":") + position);
  }
}
