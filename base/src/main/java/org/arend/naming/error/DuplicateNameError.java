package org.arend.naming.error;

import org.arend.ext.error.SourceInfo;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.naming.reference.Referable;

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
    SourceInfo sourceInfo = SourceInfo.getSourceInfo(previous);
    if (sourceInfo == null) {
      return DocFactory.hList(DocFactory.text(text + ": "), DocFactory.refDoc(previous));
    }

    String module = sourceInfo.moduleTextRepresentation();
    if (module != null) {
      SourceInfo sourceInfo1 = SourceInfo.getSourceInfo(referable);
      String module1 = sourceInfo1 != null ? sourceInfo1.moduleTextRepresentation() : null;
      if (module.equals(module1)) {
        module = null;
      }
    }

    String position = sourceInfo.positionTextRepresentation();
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
