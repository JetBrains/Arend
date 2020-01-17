package org.arend.naming.error;

import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.term.NameRenaming;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ExistingOpenedNameError extends NamingError {
  public ExistingOpenedNameError(NameRenaming cause) {
    super(Level.WARNING, "", cause);
  }

  @Override
  public NameRenaming getCause() {
    return (NameRenaming) super.getCause();
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    NameRenaming renaming = getCause();
    String newName = renaming.getName();
    return hList(text("Definition '"), refDoc(renaming.getOldReference()), text("' is not imported since " + (newName != null ? "'" + newName + "'" : "it") + " is defined in this module"));
  }
}
